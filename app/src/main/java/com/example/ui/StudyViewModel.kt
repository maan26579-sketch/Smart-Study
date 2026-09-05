package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlaybackState
import com.example.audio.LectureAudioPlayer
import com.example.audio.LectureAudioRecorder
import com.example.audio.RecordingState
import com.example.data.local.StudyDatabase
import com.example.data.model.Flashcard
import com.example.data.model.LectureSession
import com.example.data.model.QuizQuestion
import com.example.data.model.SampleMaterials
import com.example.data.model.StudySample
import com.example.data.repository.StudyRepository
import com.example.data.repository.SyncReport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "StudyViewModel"
    private val database = StudyDatabase.getInstance(application)
    private val repository = StudyRepository(
        sessionDao = database.lectureSessionDao(),
        flashcardDao = database.flashcardDao(),
        quizDao = database.quizDao()
    )

    val audioRecorder = LectureAudioRecorder(application)
    val audioPlayer = LectureAudioPlayer(application)

    val recordingState: StateFlow<RecordingState> = audioRecorder.recordingState
    val playbackState: StateFlow<AudioPlaybackState> = audioPlayer.playbackState

    val searchQuery = MutableStateFlow("")
    val selectedSubjectFilter = MutableStateFlow<String?>(null)

    val isProcessingAi = MutableStateFlow(false)
    val processingStatusMessage = MutableStateFlow("Processing material...")

    val isSyncing = MutableStateFlow(false)
    val syncReport = MutableStateFlow<SyncReport?>(null)

    val exportedDeckJson = MutableStateFlow<String?>(null)

    private val _selectedSessionId = MutableStateFlow<Long?>(null)
    val selectedSessionId: StateFlow<Long?> = _selectedSessionId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateStarterDataIfNeeded()
        }
    }

    val filteredSessions: StateFlow<List<LectureSession>> = combine(
        repository.allSessions,
        searchQuery,
        selectedSubjectFilter
    ) { sessions, query, subject ->
        sessions.filter { s ->
            val matchesSubject = subject == null || s.subject.equals(subject, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    s.title.contains(query, ignoreCase = true) ||
                    s.subject.contains(query, ignoreCase = true) ||
                    s.summary.contains(query, ignoreCase = true) ||
                    s.rawContent.contains(query, ignoreCase = true)
            matchesSubject && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSession: StateFlow<LectureSession?> = _selectedSessionId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getSession(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeFlashcards: StateFlow<List<Flashcard>> = _selectedSessionId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getFlashcards(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeQuiz: StateFlow<List<QuizQuestion>> = _selectedSessionId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getQuiz(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectSession(sessionId: Long?) {
        _selectedSessionId.value = sessionId
        audioPlayer.stop()
    }

    // Audio Recording Workflow
    fun startLectureRecording(permissionGranted: Boolean) {
        audioRecorder.startRecording(permissionGranted)
    }

    fun pauseLectureRecording() {
        audioRecorder.pauseRecording()
    }

    fun resumeLectureRecording() {
        audioRecorder.resumeRecording()
    }

    fun stopAndProcessLectureRecording(lectureNotesOrTitle: String = "") {
        val duration = audioRecorder.recordingState.value.durationSeconds
        val audioFile = audioRecorder.stopRecording()

        viewModelScope.launch {
            isProcessingAi.value = true
            processingStatusMessage.value = "Analyzing lecture audio and transcribing speech notes..."

            val content = if (lectureNotesOrTitle.isNotBlank()) {
                lectureNotesOrTitle
            } else {
                "Recorded University Lecture Audio (${duration / 60}m ${duration % 60}s). Key discussion points, professor derivations, and conceptual examples."
            }

            try {
                val newId = repository.processAndSaveMaterial(
                    rawContent = content,
                    sourceType = "AUDIO_LECTURE",
                    audioPath = audioFile?.absolutePath,
                    audioDurationSeconds = duration
                )
                _selectedSessionId.value = newId
            } catch (e: Exception) {
                Log.e(tag, "Failed to process lecture audio: ${e.message}", e)
            } finally {
                isProcessingAi.value = false
            }
        }
    }

    // Whiteboard / Image Scan Workflow
    fun processWhiteboardImage(
        uri: Uri?,
        notesContext: String = "",
        subjectHint: String? = null
    ) {
        viewModelScope.launch {
            isProcessingAi.value = true
            processingStatusMessage.value = "Scanning whiteboard: Running vision OCR on diagrams and handwriting..."

            var bitmap: Bitmap? = null
            if (uri != null) {
                try {
                    val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
                    bitmap = BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    Log.w(tag, "Could not open image uri: ${e.message}")
                }
            }

            val content = if (notesContext.isNotBlank()) notesContext
            else "Whiteboard Notes & Equations. Scanned handwriting, structural chemistry/physics mechanics diagrams, and conceptual proofs."

            try {
                val newId = repository.processAndSaveMaterial(
                    rawContent = content,
                    sourceType = "WHITEBOARD_SCAN",
                    bitmap = bitmap,
                    subjectHint = subjectHint,
                    imageUri = uri?.toString()
                )
                _selectedSessionId.value = newId
            } catch (e: Exception) {
                Log.e(tag, "Error processing whiteboard: ${e.message}", e)
            } finally {
                isProcessingAi.value = false
            }
        }
    }

    // Process Quick Sample Preset
    fun processSampleMaterial(sample: StudySample) {
        viewModelScope.launch {
            isProcessingAi.value = true
            processingStatusMessage.value = "Synthesizing ${sample.subject} ${if (sample.type == "AUDIO_LECTURE") "Audio Lecture" else "Whiteboard Scan"}..."

            try {
                val newId = repository.processAndSaveMaterial(
                    rawContent = sample.content,
                    sourceType = sample.type,
                    subjectHint = sample.subject,
                    audioDurationSeconds = if (sample.type == "AUDIO_LECTURE") 210 else 0
                )
                _selectedSessionId.value = newId
            } catch (e: Exception) {
                Log.e(tag, "Error synthesizing sample: ${e.message}", e)
            } finally {
                isProcessingAi.value = false
            }
        }
    }

    fun updateFlashcardMastery(cardId: Long, isMastered: Boolean) {
        viewModelScope.launch {
            repository.updateFlashcardMastery(cardId, isMastered)
        }
    }

    fun recordQuizAnswer(questionId: Long, selectedIndex: Int) {
        viewModelScope.launch {
            repository.recordQuizAnswer(questionId, selectedIndex)
        }
    }

    fun resetQuiz() {
        val sessionId = _selectedSessionId.value ?: return
        viewModelScope.launch {
            repository.resetQuiz(sessionId)
        }
    }

    fun toggleBookmark(session: LectureSession) {
        viewModelScope.launch {
            repository.toggleBookmark(session)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_selectedSessionId.value == sessionId) {
                _selectedSessionId.value = null
            }
        }
    }

    fun syncAllDevices() {
        viewModelScope.launch {
            isSyncing.value = true
            try {
                // Simulate cloud sync roundtrip
                kotlinx.coroutines.delay(1200)
                val report = repository.syncAllDevices()
                syncReport.value = report
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun exportDeckBackup() {
        viewModelScope.launch {
            val json = repository.exportAllDecksJson()
            exportedDeckJson.value = json
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        audioRecorder.stopRecording()
    }
}
