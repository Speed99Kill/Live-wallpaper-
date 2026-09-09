package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.WallpaperConfig
import com.example.sensors.MotionData
import com.example.sensors.MotionSensorTracker
import com.example.wallpaper.renderer.WallpaperEngineRenderer

@Composable
fun InteractiveWallpaperCanvas(
    config: WallpaperConfig,
    motionTracker: MotionSensorTracker,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
    val renderer = remember(config.engineType) { WallpaperEngineRenderer() }
    
    // Only subscribe to real-time motion data if interactive, to prevent heavy recompositions on thumbnails
    val activeMotionFlow = remember(isInteractive, motionTracker) {
        if (isInteractive) motionTracker.motionFlow else kotlinx.coroutines.flow.flowOf(MotionData(0f, 0f, 0f))
    }
    val motionData by activeMotionFlow.collectAsStateWithLifecycle(initialValue = MotionData(0f, 0f, 0f))

    var totalTimeSec by remember { mutableFloatStateOf(0f) }
    var lastNanoTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isInteractive) {
        if (isInteractive) {
            lastNanoTime = System.nanoTime()
            while (true) {
                withFrameNanos { now ->
                    val dt = if (lastNanoTime > 0L) {
                        ((now - lastNanoTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    } else {
                        0.016f
                    }
                    lastNanoTime = now
                    totalTimeSec += dt
                }
            }
        } else {
            // Low-end device optimization: For static thumbnails, stop the infinite 60fps loop.
            totalTimeSec = 2.5f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .then(
                if (isInteractive) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    renderer.onTouchDown(0, offset.x, offset.y)
                                    tryAwaitRelease()
                                    renderer.onTouchUp(0)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    renderer.onTouchDown(0, offset.x, offset.y)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    renderer.onTouchMove(0, change.position.x, change.position.y)
                                },
                                onDragEnd = {
                                    renderer.onTouchUp(0)
                                },
                                onDragCancel = {
                                    renderer.onTouchUp(0)
                                }
                            )
                        }
                } else Modifier
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                val width = size.width.toInt()
                val height = size.height.toInt()

                renderer.render(
                    canvas = nativeCanvas,
                    width = width,
                    height = height,
                    timeSec = totalTimeSec,
                    deltaTime = 0.016f,
                    roll = motionData.roll,
                    pitch = motionData.pitch,
                    config = config
                )
            }
        }
    }
}

/**
 * Renders a high-resolution snapshot bitmap of the current wallpaper configuration.
 */
fun renderWallpaperSnapshot(
    config: WallpaperConfig,
    width: Int = 1080,
    height: Int = 2400,
    roll: Float = 0f,
    pitch: Float = 0f,
    timeSec: Float = 2.5f
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val renderer = WallpaperEngineRenderer()
    renderer.render(
        canvas = canvas,
        width = width,
        height = height,
        timeSec = timeSec,
        deltaTime = 0.016f,
        roll = roll,
        pitch = pitch,
        config = config
    )
    return bitmap
}
