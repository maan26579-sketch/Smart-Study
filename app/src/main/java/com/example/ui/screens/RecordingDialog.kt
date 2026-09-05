package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RecordingState
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseMistake
import java.util.Locale

@Composable
fun RecordingDialog(
    recordingState: RecordingState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopAndProcess: (lectureNotes: String) -> Unit,
    onCancel: () -> Unit
) {
    var lectureNotes by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RoseMistake.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = RoseMistake,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Lecture Audio Recorder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Recording status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (recordingState.isRecording && !recordingState.isPaused) {
                        Box(
                            modifier = Modifier
                                .size((10 * pulseScale).dp)
                                .clip(CircleShape)
                                .background(RoseMistake)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RECORDING LIVE AUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RoseMistake
                        )
                    } else if (recordingState.isPaused) {
                        Text(
                            text = "RECORDING PAUSED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Big timer
                val m = recordingState.durationSeconds / 60
                val s = recordingState.durationSeconds % 60
                val timeStr = String.format(Locale.US, "%02d:%02d", m, s)
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Audio Waveform
                val primaryColor = MaterialTheme.colorScheme.primary
                val waveHistory = recordingState.waveformHistory

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(8.dp)
                    ) {
                        if (waveHistory.isNotEmpty()) {
                            val barCount = waveHistory.size
                            val spacing = size.width / barCount.toFloat()
                            val barWidth = spacing * 0.65f

                            for (i in 0 until barCount) {
                                val amp = waveHistory[i].coerceIn(0.1f, 1.0f)
                                val barHeight = amp * (size.height - 4.dp.toPx())
                                val x = i * spacing
                                val y = (size.height - barHeight) / 2f

                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Optional live notes/topic field
                OutlinedTextField(
                    value = lectureNotes,
                    onValueChange = { lectureNotes = it },
                    label = { Text("Lecture notes / Topic hint (Optional)") },
                    placeholder = { Text("e.g., Quantum Mechanics, Dr. Chen, Lecture 5") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lecture_notes_input"),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recorder Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            if (recordingState.isPaused) onResume() else onPause()
                        },
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (recordingState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (recordingState.isPaused) "Resume" else "Pause"
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Button(
                        onClick = { onStopAndProcess(lectureNotes) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoseMistake,
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("stop_recording_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finish & Convert", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}
