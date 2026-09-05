package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f
)

class LectureAudioPlayer(private val context: Context) {

    private val tag = "LectureAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private var scope = CoroutineScope(Dispatchers.Main)

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var isSimulatedPlayback = false

    fun loadAndPlay(filePath: String?, fallbackDurationSeconds: Int) {
        stop()

        val file = if (!filePath.isNullOrBlank()) File(filePath) else null
        if (file != null && file.exists() && file.length() > 0) {
            try {
                val mp = MediaPlayer()
                mp.setDataSource(file.absolutePath)
                mp.prepare()
                val duration = mp.duration / 1000
                _playbackState.value = AudioPlaybackState(
                    isPlaying = true,
                    currentPositionSeconds = 0,
                    totalDurationSeconds = if (duration > 0) duration else fallbackDurationSeconds,
                    playbackSpeed = 1.0f
                )
                mp.start()
                mediaPlayer = mp
                isSimulatedPlayback = false
                startPositionTracker()
                return
            } catch (e: Exception) {
                Log.w(tag, "Hardware MediaPlayer playback failed (${e.message}), using virtual lecture player")
            }
        }

        // Virtual simulated audio player for preloaded/sample audio lectures
        isSimulatedPlayback = true
        _playbackState.value = AudioPlaybackState(
            isPlaying = true,
            currentPositionSeconds = 0,
            totalDurationSeconds = if (fallbackDurationSeconds > 0) fallbackDurationSeconds else 180,
            playbackSpeed = 1.0f
        )
        startPositionTracker()
    }

    fun togglePlayPause() {
        val current = _playbackState.value
        if (current.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        if (!isSimulatedPlayback) {
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) {
                Log.w(tag, "Pause error: ${e.message}")
            }
        }
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun resume() {
        if (!isSimulatedPlayback) {
            try {
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.w(tag, "Resume error: ${e.message}")
            }
        }
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
    }

    fun seekTo(seconds: Int) {
        val bounded = seconds.coerceIn(0, _playbackState.value.totalDurationSeconds)
        if (!isSimulatedPlayback) {
            try {
                mediaPlayer?.seekTo(bounded * 1000)
            } catch (e: Exception) {
                Log.w(tag, "Seek error: ${e.message}")
            }
        }
        _playbackState.value = _playbackState.value.copy(currentPositionSeconds = bounded)
    }

    fun cycleSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
        val currentSpeed = _playbackState.value.playbackSpeed
        val nextIndex = (speeds.indexOf(currentSpeed) + 1) % speeds.size
        val newSpeed = speeds[nextIndex]

        if (!isSimulatedPlayback && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.playbackParams = PlaybackParams().apply { speed = newSpeed }
            } catch (e: Exception) {
                Log.w(tag, "Set speed error: ${e.message}")
            }
        }
        _playbackState.value = _playbackState.value.copy(playbackSpeed = newSpeed)
    }

    private fun startPositionTracker() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                val speed = _playbackState.value.playbackSpeed
                val stepMs = (1000 / speed).toLong().coerceAtLeast(200L)
                delay(stepMs)

                if (_playbackState.value.isPlaying) {
                    if (!isSimulatedPlayback && mediaPlayer != null) {
                        try {
                            val pos = (mediaPlayer?.currentPosition ?: 0) / 1000
                            _playbackState.value = _playbackState.value.copy(currentPositionSeconds = pos)
                            if (pos >= _playbackState.value.totalDurationSeconds) {
                                _playbackState.value = _playbackState.value.copy(
                                    isPlaying = false,
                                    currentPositionSeconds = _playbackState.value.totalDurationSeconds
                                )
                                break
                            }
                        } catch (_: Exception) {
                            stepSimulated()
                        }
                    } else {
                        stepSimulated()
                    }
                }
            }
        }
    }

    private fun stepSimulated() {
        val current = _playbackState.value.currentPositionSeconds
        val total = _playbackState.value.totalDurationSeconds
        if (current + 1 >= total) {
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                currentPositionSeconds = total
            )
        } else {
            _playbackState.value = _playbackState.value.copy(currentPositionSeconds = current + 1)
        }
    }

    fun stop() {
        playbackJob?.cancel()
        if (!isSimulatedPlayback) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                Log.w(tag, "Stop player error: ${e.message}")
            }
        }
        mediaPlayer = null
        _playbackState.value = AudioPlaybackState()
    }
}
