package com.example.wallpaper

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.example.data.WallpaperPreferences
import com.example.model.WallpaperConfig
import com.example.sensors.MotionSensorTracker
import com.example.wallpaper.renderer.WallpaperEngineRenderer

class InteractiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return InteractiveEngine()
    }

    inner class InteractiveEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val renderer = WallpaperEngineRenderer()
        private lateinit var preferences: WallpaperPreferences
        private lateinit var motionTracker: MotionSensorTracker
        private var currentConfig: WallpaperConfig? = null

        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false
        private var width = 0
        private var height = 0
        private var lastFrameTime = System.nanoTime()
        private var totalElapsedTime = 0f

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (isVisible) {
                    handler.postDelayed(this, 16) // Target ~60 FPS
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            preferences = WallpaperPreferences(this@InteractiveWallpaperService)
            motionTracker = MotionSensorTracker(this@InteractiveWallpaperService)
            currentConfig = preferences.getActiveWallpaper()
            preferences.registerChangeListener(this)
            setTouchEventsEnabled(true)
        }

        override fun onDestroy() {
            super.onDestroy()
            preferences.unregisterChangeListener(this)
            motionTracker.stopListening()
            handler.removeCallbacks(drawRunnable)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                currentConfig = preferences.getActiveWallpaper()
                motionTracker.configure(
                    currentConfig?.sensorSensitivity ?: 1.2f,
                    currentConfig?.invertMotion ?: false
                )
                motionTracker.startListening()
                lastFrameTime = System.nanoTime()
                handler.post(drawRunnable)
            } else {
                motionTracker.stopListening()
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
            drawFrame()
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    renderer.onTouchDown(0, event.x, event.y)
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val idx = event.actionIndex
                    if (idx < 3) {
                        renderer.onTouchDown(idx, event.getX(idx), event.getY(idx))
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until event.pointerCount.coerceAtMost(3)) {
                        renderer.onTouchMove(i, event.getX(i), event.getY(i))
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val idx = event.actionIndex
                    if (idx < 3) {
                        renderer.onTouchUp(idx)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    for (i in 0 until 3) {
                        renderer.onTouchUp(i)
                    }
                }
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            currentConfig = preferences.getActiveWallpaper()
            currentConfig?.let {
                motionTracker.configure(it.sensorSensitivity, it.invertMotion)
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            if (width <= 0 || height <= 0) return

            val now = System.nanoTime()
            val deltaTime = ((now - lastFrameTime) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
            lastFrameTime = now
            totalElapsedTime += deltaTime

            val config = currentConfig ?: preferences.getActiveWallpaper()
            val motion = motionTracker.motionFlow.value

            var canvas = holder.lockHardwareCanvas()
            try {
                if (canvas != null) {
                    renderer.render(
                        canvas = canvas,
                        width = width,
                        height = height,
                        timeSec = totalElapsedTime,
                        deltaTime = deltaTime,
                        roll = motion.roll,
                        pitch = motion.pitch,
                        config = config
                    )
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
