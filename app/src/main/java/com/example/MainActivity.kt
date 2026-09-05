package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.StudyViewModel
import com.example.ui.components.SyncDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecordingDialog
import com.example.ui.screens.ScanWhiteboardDialog
import com.example.ui.screens.SessionDetailScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                StudyApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StudyApp(viewModel: StudyViewModel) {
    val context = LocalContext.current

    val sessions by viewModel.filteredSessions.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val activeFlashcards by viewModel.activeFlashcards.collectAsState()
    val activeQuiz by viewModel.activeQuiz.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSubject by viewModel.selectedSubjectFilter.collectAsState()

    val recordingState by viewModel.recordingState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val isProcessingAi by viewModel.isProcessingAi.collectAsState()
    val processingMessage by viewModel.processingStatusMessage.collectAsState()

    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncReport by viewModel.syncReport.collectAsState()
    val exportedDeckJson by viewModel.exportedDeckJson.collectAsState()

    var showRecordDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    // Audio Permission Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.startLectureRecording(isGranted)
        showRecordDialog = true
    }

    fun startRecordingFlow() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            viewModel.startLectureRecording(true)
            showRecordDialog = true
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Back handler for detail view
    BackHandler(enabled = activeSession != null) {
        viewModel.selectSession(null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeSession == null) {
            HomeScreen(
                sessions = sessions,
                searchQuery = searchQuery,
                selectedSubject = selectedSubject,
                onSearchQueryChange = { viewModel.searchQuery.value = it },
                onSubjectSelected = { viewModel.selectedSubjectFilter.value = it },
                onSelectSession = { id ->
                    val selected = sessions.find { it.id == id }
                    viewModel.selectSession(id)
                    if (selected != null && (selected.sourceType == "AUDIO_LECTURE" || selected.audioDurationSeconds > 0)) {
                        viewModel.audioPlayer.loadAndPlay(selected.audioFilePath, selected.audioDurationSeconds)
                    }
                },
                onToggleBookmark = { viewModel.toggleBookmark(it) },
                onOpenRecordDialog = { startRecordingFlow() },
                onOpenScanDialog = { showScanDialog = true },
                onOpenSyncDialog = { showSyncDialog = true },
                onPickSample = { sample ->
                    viewModel.processSampleMaterial(sample)
                }
            )
        } else {
            val session = activeSession!!
            SessionDetailScreen(
                session = session,
                flashcards = activeFlashcards,
                quiz = activeQuiz,
                playbackState = playbackState,
                onBack = { viewModel.selectSession(null) },
                onToggleBookmark = { viewModel.toggleBookmark(session) },
                onDelete = { viewModel.deleteSession(session.id) },
                onMasteryChanged = { cardId, mastered ->
                    viewModel.updateFlashcardMastery(cardId, mastered)
                },
                onQuizAnswerSelected = { qId, optIdx ->
                    viewModel.recordQuizAnswer(qId, optIdx)
                },
                onResetQuiz = { viewModel.resetQuiz() },
                onTogglePlayPause = { viewModel.audioPlayer.togglePlayPause() },
                onSeekTo = { seconds -> viewModel.audioPlayer.seekTo(seconds) },
                onCycleSpeed = { viewModel.audioPlayer.cycleSpeed() }
            )
        }

        // Recording Dialog
        if (showRecordDialog) {
            RecordingDialog(
                recordingState = recordingState,
                onPause = { viewModel.pauseLectureRecording() },
                onResume = { viewModel.resumeLectureRecording() },
                onStopAndProcess = { notes ->
                    showRecordDialog = false
                    viewModel.stopAndProcessLectureRecording(notes)
                },
                onCancel = {
                    showRecordDialog = false
                    viewModel.audioRecorder.stopRecording()
                }
            )
        }

        // Scan Whiteboard Dialog
        if (showScanDialog) {
            ScanWhiteboardDialog(
                onProcessWhiteboard = { uri, notes, subject ->
                    showScanDialog = false
                    viewModel.processWhiteboardImage(uri, notes, subject)
                },
                onProcessSample = { sample ->
                    showScanDialog = false
                    viewModel.processSampleMaterial(sample)
                },
                onDismiss = { showScanDialog = false }
            )
        }

        // Sync Dialog
        if (showSyncDialog) {
            SyncDialog(
                isSyncing = isSyncing,
                syncReport = syncReport,
                onTriggerSync = { viewModel.syncAllDevices() },
                onExportBackup = { viewModel.exportDeckBackup() },
                onDismiss = { showSyncDialog = false }
            )
        }

        // AI Processing Modal
        if (isProcessingAi) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Smart Study AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = processingMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Export Deck JSON Backup Dialog
        if (exportedDeckJson != null) {
            AlertDialog(
                onDismissRequest = { viewModel.exportedDeckJson.value = null },
                title = { Text("Study Deck JSON Export") },
                text = {
                    Column {
                        Text(
                            text = "Full portable backup ready for sync across your phone, tablet, and web:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Text(
                                text = exportedDeckJson!!.take(800) + "\n... [Full JSON formatted for import]",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Decks JSON", exportedDeckJson))
                            Toast.makeText(context, "Copied backup JSON to clipboard!", Toast.LENGTH_SHORT).show()
                            viewModel.exportedDeckJson.value = null
                        }
                    ) {
                        Text("Copy JSON")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.exportedDeckJson.value = null }) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }
}
