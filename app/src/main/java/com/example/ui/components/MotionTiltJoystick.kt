package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensors.MotionSensorTracker
import kotlin.math.roundToInt

@Composable
fun MotionTiltJoystick(
    motionTracker: MotionSensorTracker,
    modifier: Modifier = Modifier
) {
    val motionData by motionTracker.motionFlow.collectAsStateWithLifecycle()
    val isSimulating = motionTracker.isSimulationActive

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("motion_tilt_controller"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (motionData.isHardwareAvailable && !isSimulating) {
                            Icons.Default.Sensors
                        } else {
                            Icons.Default.SensorsOff
                        },
                        contentDescription = "Sensor status",
                        tint = if (motionData.isHardwareAvailable && !isSimulating) {
                            Color(0xFF00E5FF)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSimulating) "Virtual Gyro (Drag Knob)" else "Motion Sensor Active",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tilt X: ${"%.2f".format(motionData.roll)}  |  Tilt Y: ${"%.2f".format(motionData.pitch)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isSimulating,
                        onClick = {
                            motionTracker.isSimulationActive = !isSimulating
                            if (!motionTracker.isSimulationActive) {
                                motionTracker.simulatedRoll = 0f
                                motionTracker.simulatedPitch = 0f
                            }
                        },
                        label = {
                            Text(
                                if (isSimulating) "Using Virtual Tilt" else "Test Tilt Joystick",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("toggle_simulation_button")
                    )

                    if (isSimulating) {
                        IconButton(
                            onClick = {
                                motionTracker.simulatedRoll = 0f
                                motionTracker.simulatedPitch = 0f
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset tilt",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Virtual Tilt Joystick Pad
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF141926))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                    .pointerInput(isSimulating) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                motionTracker.isSimulationActive = true
                                val normX = ((offset.x - 40.dp.toPx()) / 35.dp.toPx()).coerceIn(-1.2f, 1.2f)
                                val normY = ((offset.y - 40.dp.toPx()) / 35.dp.toPx()).coerceIn(-1.2f, 1.2f)
                                motionTracker.simulatedRoll = normX
                                motionTracker.simulatedPitch = normY
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val normX = ((change.position.x - 40.dp.toPx()) / 35.dp.toPx()).coerceIn(-1.2f, 1.2f)
                                val normY = ((change.position.y - 40.dp.toPx()) / 35.dp.toPx()).coerceIn(-1.2f, 1.2f)
                                motionTracker.simulatedRoll = normX
                                motionTracker.simulatedPitch = normY
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Crosshairs
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(cx, 8f),
                        end = Offset(cx, size.height - 8f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(8f, cy),
                        end = Offset(size.width - 8f, cy),
                        strokeWidth = 1f
                    )
                }

                // Joystick thumb indicator
                val knobOffsetX = (motionData.roll * 24.dp.value).coerceIn(-28f, 28f)
                val knobOffsetY = (motionData.pitch * 24.dp.value).coerceIn(-28f, 28f)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(knobOffsetX.roundToInt(), knobOffsetY.roundToInt()) }
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSimulating) MaterialTheme.colorScheme.primary else Color(0xFF00E5FF)
                        )
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }
    }
}
