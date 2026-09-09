package com.example.wallpaper.renderer

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.model.ColorPalette
import com.example.model.TouchInteraction
import com.example.model.WallpaperConfig
import com.example.model.WallpaperEngineType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class TouchPoint(
    var x: Float = 0f,
    var y: Float = 0f,
    var isActive: Boolean = false,
    var rippleRadius: Float = 0f,
    var rippleAlpha: Float = 0f
)

class WallpaperEngineRenderer {

    // Common Paints
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // Touch state
    val touchPoints = Array(3) { TouchPoint() }

    // Particles for Cosmic & Kinetic
    private class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var size: Float = 4f,
        var alpha: Float = 1f,
        var layer: Int = 1,
        var pulseOffset: Float = 0f
    )

    private val maxParticles = 120
    private val particles = Array(maxParticles) { Particle() }
    private var initializedWidth = 0
    private var initializedHeight = 0

    // Digital rain streams
    private class RainColumn(
        var x: Float = 0f,
        var y: Float = 0f,
        var speed: Float = 300f,
        var length: Int = 14,
        var charSeed: Int = 0
    )

    private val rainColumns = Array(32) { RainColumn() }

    // Wave paths
    private val wavePath = Path()

    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeSec: Float,
        deltaTime: Float,
        roll: Float,  // -1.0 to 1.0 (device tilt X)
        pitch: Float, // -1.0 to 1.0 (device tilt Y)
        config: WallpaperConfig
    ) {
        if (width <= 0 || height <= 0) return

        if (initializedWidth != width || initializedHeight != height) {
            initDimensions(width, height)
        }

        val palette = config.palette
        val speed = config.animationSpeed
        val sensitivity = config.sensorSensitivity
        val effectiveRoll = roll * sensitivity
        val effectivePitch = pitch * sensitivity

        // Update ripple animations
        for (tp in touchPoints) {
            if (tp.isActive || tp.rippleRadius > 0f) {
                tp.rippleRadius += deltaTime * 500f * speed
                tp.rippleAlpha = (1f - (tp.rippleRadius / (width * 0.75f))).coerceIn(0f, 1f)
                if (tp.rippleAlpha <= 0.01f && !tp.isActive) {
                    tp.rippleRadius = 0f
                }
            }
        }

        when (config.engineType) {
            WallpaperEngineType.COSMIC_GRAVITY -> renderCosmic(
                canvas, width, height, timeSec, deltaTime, effectiveRoll, effectivePitch, config, palette
            )
            WallpaperEngineType.NEON_SYNTHWAVE -> renderSynthwave(
                canvas, width, height, timeSec, deltaTime, effectiveRoll, effectivePitch, config, palette
            )
            WallpaperEngineType.AURORA_LIQUID -> renderAurora(
                canvas, width, height, timeSec, deltaTime, effectiveRoll, effectivePitch, config, palette
            )
            WallpaperEngineType.KINETIC_MATRIX -> renderKinetic(
                canvas, width, height, timeSec, deltaTime, effectiveRoll, effectivePitch, config, palette
            )
            WallpaperEngineType.DIGITAL_RAIN -> renderDigitalRain(
                canvas, width, height, timeSec, deltaTime, effectiveRoll, effectivePitch, config, palette
            )
            WallpaperEngineType.MYSTIC_FIREFLIES -> renderFireflies(
                canvas, width, height, timeSec, deltaTime, effectiveRoll, effectivePitch, config, palette
            )
        }

        // Render universal touch ripple effect if active
        renderTouchRipples(canvas, palette)
    }

    private fun initDimensions(width: Int, height: Int) {
        initializedWidth = width
        initializedHeight = height
        val rnd = Random(42)

        for (i in particles.indices) {
            particles[i].x = rnd.nextFloat() * width
            particles[i].y = rnd.nextFloat() * height
            particles[i].vx = (rnd.nextFloat() - 0.5f) * 40f
            particles[i].vy = (rnd.nextFloat() - 0.5f) * 40f
            particles[i].size = rnd.nextFloat() * 4f + 2f
            particles[i].layer = rnd.nextInt(1, 4)
            particles[i].pulseOffset = rnd.nextFloat() * 6.28f
        }

        val colSpacing = width.toFloat() / rainColumns.size
        for (i in rainColumns.indices) {
            rainColumns[i].x = i * colSpacing + colSpacing * 0.5f
            rainColumns[i].y = rnd.nextFloat() * height
            rainColumns[i].speed = rnd.nextFloat() * 250f + 150f
            rainColumns[i].length = rnd.nextInt(8, 20)
            rainColumns[i].charSeed = rnd.nextInt()
        }
    }

    // 1. COSMIC GRAVITY ENGINE
    private fun renderCosmic(
        canvas: Canvas,
        w: Int,
        h: Int,
        time: Float,
        dt: Float,
        roll: Float,
        pitch: Float,
        config: WallpaperConfig,
        palette: ColorPalette
    ) {
        // Dynamic background gradient with cosmic nebula glow
        val cx = w * 0.5f + roll * (w * 0.35f)
        val cy = h * 0.45f + pitch * (h * 0.25f)
        val nebulaRadius = w * 0.9f

        val bgShader = RadialGradient(
            cx, cy, nebulaRadius,
            intArrayOf(
                palette.glowColor.toInt(),
                palette.backgroundEnd.toInt(),
                palette.backgroundStart.toInt()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), backgroundPaint)

        val activeCount = (particles.size * config.particleDensity).toInt().coerceIn(20, maxParticles)
        val speed = config.animationSpeed

        for (i in 0 until activeCount) {
            val p = particles[i]
            val parallax = p.layer * 0.35f

            // Gravitational pull / repulsion from touch
            var ax = roll * 80f * parallax
            var ay = pitch * 80f * parallax

            for (tp in touchPoints) {
                if (tp.isActive) {
                    val dx = tp.x - p.x
                    val dy = tp.y - p.y
                    val dist = hypot(dx, dy).coerceAtLeast(20f)
                    val force = (12000f / (dist * dist)).coerceAtMost(350f) * speed
                    val sign = if (config.touchInteraction == TouchInteraction.REPEL) -1f else 1f
                    ax += (dx / dist) * force * sign
                    ay += (dy / dist) * force * sign
                }
            }

            p.vx += ax * dt
            p.vy += ay * dt
            p.vx *= 0.96f // drag
            p.vy *= 0.96f

            p.x += (p.vx + (sin(time * speed + p.pulseOffset) * 15f)) * dt * speed
            p.y += (p.vy + (cos(time * speed + p.pulseOffset) * 15f)) * dt * speed

            // Screen boundary wrapping
            if (p.x < -20) p.x = w + 20f
            if (p.x > w + 20) p.x = -20f
            if (p.y < -20) p.y = h + 20f
            if (p.y > h + 20) p.y = -20f

            // Render star
            val pulse = (sin(time * 3f * speed + p.pulseOffset) * 0.4f + 0.6f)
            val currentSize = p.size * pulse

            primaryPaint.color = when (p.layer) {
                3 -> palette.primaryColor.toInt()
                2 -> palette.accentColor.toInt()
                else -> palette.secondaryColor.toInt()
            }
            primaryPaint.alpha = (pulse * 255).toInt().coerceIn(40, 255)
            canvas.drawCircle(p.x, p.y, currentSize, primaryPaint)

            // Star halo for layer 3
            if (p.layer == 3) {
                glowPaint.color = palette.glowColor.toInt()
                glowPaint.alpha = (pulse * 90).toInt()
                canvas.drawCircle(p.x, p.y, currentSize * 2.8f, glowPaint)
            }
        }
    }

    // 2. SYNTHWAVE 3D GRID
    private fun renderSynthwave(
        canvas: Canvas,
        w: Int,
        h: Int,
        time: Float,
        dt: Float,
        roll: Float,
        pitch: Float,
        config: WallpaperConfig,
        palette: ColorPalette
    ) {
        val speed = config.animationSpeed
        val horizonY = (h * 0.52f + pitch * (h * 0.18f)).coerceIn(h * 0.3f, h * 0.7f)
        val vanishingX = (w * 0.5f + roll * (w * 0.35f)).coerceIn(w * 0.1f, w * 0.9f)

        // Sky gradient
        val skyShader = LinearGradient(
            0f, 0f, 0f, horizonY,
            intArrayOf(palette.backgroundStart.toInt(), palette.backgroundEnd.toInt()),
            null, Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = skyShader
        canvas.drawRect(0f, 0f, w.toFloat(), horizonY, backgroundPaint)

        // Retro Sun
        val sunRadius = w * 0.26f
        val sunY = horizonY - sunRadius * 0.45f
        val sunShader = LinearGradient(
            vanishingX, sunY - sunRadius, vanishingX, sunY + sunRadius,
            intArrayOf(palette.secondaryColor.toInt(), palette.primaryColor.toInt()),
            null, Shader.TileMode.CLAMP
        )
        primaryPaint.shader = sunShader
        canvas.drawCircle(vanishingX, sunY, sunRadius, primaryPaint)

        // Horizontal sun grill cuts
        linePaint.shader = null
        linePaint.color = palette.backgroundEnd.toInt()
        linePaint.strokeWidth = 5f
        for (i in 1..7) {
            val cutY = sunY + (i * 12f)
            if (cutY < horizonY) {
                linePaint.strokeWidth = 3f + i * 1.5f
                canvas.drawLine(vanishingX - sunRadius, cutY, vanishingX + sunRadius, cutY, linePaint)
            }
        }

        // Ground floor
        backgroundPaint.shader = null
        backgroundPaint.color = palette.backgroundStart.toInt()
        canvas.drawRect(0f, horizonY, w.toFloat(), h.toFloat(), backgroundPaint)

        // 3D Perspective Grid
        linePaint.color = palette.primaryColor.toInt()
        linePaint.alpha = 180

        // Perspective longitudinal lines radiating from vanishing point
        val numLines = 14
        for (i in -numLines / 2..numLines / 2) {
            val groundEndX = vanishingX + (i * (w * 0.16f))
            canvas.drawLine(vanishingX, horizonY, groundEndX, h.toFloat(), linePaint)
        }

        // Advancing horizontal grid lines with exponential perspective spacing
        val scrollOffset = (time * 1.2f * speed) % 1.0f
        val numHorizontals = 12
        for (i in 0 until numHorizontals) {
            val progress = ((i + scrollOffset) / numHorizontals)
            val pSq = progress * progress // perspective depth curve
            val lineY = horizonY + pSq * (h - horizonY)
            linePaint.strokeWidth = (1f + pSq * 4f)
            linePaint.alpha = (pSq * 220).toInt().coerceIn(20, 255)
            canvas.drawLine(0f, lineY, w.toFloat(), lineY, linePaint)
        }
    }

    // 3. LIQUID AURORA WAVES
    private fun renderAurora(
        canvas: Canvas,
        w: Int,
        h: Int,
        time: Float,
        dt: Float,
        roll: Float,
        pitch: Float,
        config: WallpaperConfig,
        palette: ColorPalette
    ) {
        val speed = config.animationSpeed
        // Base dark backdrop
        val bgShader = LinearGradient(
            0f, 0f, w * 0.5f, h.toFloat(),
            palette.backgroundStart.toInt(), palette.backgroundEnd.toInt(),
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), backgroundPaint)

        // 4 flowing fluid ribbon layers
        val layers = 4
        val colors = intArrayOf(
            palette.glowColor.toInt(),
            palette.primaryColor.toInt(),
            palette.accentColor.toInt(),
            palette.secondaryColor.toInt()
        )

        for (l in 0 until layers) {
            wavePath.reset()
            val baseHeight = h * (0.35f + l * 0.14f) + pitch * (h * 0.15f)
            val waveSpeed = (0.8f + l * 0.4f) * speed
            val phase = time * waveSpeed + (l * 1.6f) + roll * 2f

            wavePath.moveTo(0f, h.toFloat())
            wavePath.lineTo(0f, baseHeight)

            val segments = 24
            val dx = w.toFloat() / segments
            for (i in 0..segments) {
                val x = i * dx
                val normX = x / w
                val y = baseHeight +
                        sin(normX * 4f * PI.toFloat() + phase) * (40f + l * 12f) +
                        cos(normX * 2.5f * PI.toFloat() - phase * 0.7f) * 25f
                wavePath.lineTo(x, y)
            }

            wavePath.lineTo(w.toFloat(), h.toFloat())
            wavePath.close()

            primaryPaint.shader = null
            primaryPaint.color = colors[l]
            primaryPaint.alpha = (80 + l * 35).coerceIn(40, 220)
            canvas.drawPath(wavePath, primaryPaint)
        }
    }

    // 4. KINETIC CONSTELLATION MATRIX
    private fun renderKinetic(
        canvas: Canvas,
        w: Int,
        h: Int,
        time: Float,
        dt: Float,
        roll: Float,
        pitch: Float,
        config: WallpaperConfig,
        palette: ColorPalette
    ) {
        val speed = config.animationSpeed
        backgroundPaint.shader = null
        backgroundPaint.color = palette.backgroundStart.toInt()
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), backgroundPaint)

        val activeCount = (particles.size * config.particleDensity * 0.45f).toInt().coerceIn(18, 48)
        val gravityX = roll * 140f * speed
        val gravityY = pitch * 140f * speed

        for (i in 0 until activeCount) {
            val p = particles[i]
            p.vx += gravityX * dt
            p.vy += gravityY * dt
            p.vx *= 0.98f
            p.vy *= 0.98f

            // Touch interaction
            for (tp in touchPoints) {
                if (tp.isActive) {
                    val dx = tp.x - p.x
                    val dy = tp.y - p.y
                    val dist = hypot(dx, dy).coerceAtLeast(10f)
                    if (dist < 320f) {
                        val force = (1f - dist / 320f) * 220f * speed
                        val sign = if (config.touchInteraction == TouchInteraction.REPEL) -1f else 1f
                        p.vx += (dx / dist) * force * sign * dt
                        p.vy += (dy / dist) * force * sign * dt
                    }
                }
            }

            p.x += p.vx * dt
            p.y += p.vy * dt

            // Physical bounce off walls
            if (p.x < 20) { p.x = 20f; p.vx = -p.vx * 0.8f }
            if (p.x > w - 20) { p.x = w - 20f; p.vx = -p.vx * 0.8f }
            if (p.y < 20) { p.y = 20f; p.vy = -p.vy * 0.8f }
            if (p.y > h - 20) { p.y = h - 20f; p.vy = -p.vy * 0.8f }
        }

        // Connect nearby nodes with dynamic energy lines
        val maxDist = w * 0.32f
        linePaint.color = palette.accentColor.toInt()
        for (i in 0 until activeCount) {
            for (j in i + 1 until activeCount) {
                val p1 = particles[i]
                val p2 = particles[j]
                val dist = hypot(p1.x - p2.x, p1.y - p2.y)
                if (dist < maxDist) {
                    val lineAlpha = ((1f - dist / maxDist) * 160).toInt()
                    linePaint.alpha = lineAlpha
                    linePaint.strokeWidth = (1f - dist / maxDist) * 3f
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
                }
            }
        }

        // Draw node circles with glowing core
        for (i in 0 until activeCount) {
            val p = particles[i]
            glowPaint.color = palette.glowColor.toInt()
            canvas.drawCircle(p.x, p.y, p.size * 2.5f, glowPaint)

            primaryPaint.color = palette.primaryColor.toInt()
            primaryPaint.alpha = 255
            canvas.drawCircle(p.x, p.y, p.size, primaryPaint)
        }
    }

    // 5. DIGITAL RAIN ENGINE
    private fun renderDigitalRain(
        canvas: Canvas,
        w: Int,
        h: Int,
        time: Float,
        dt: Float,
        roll: Float,
        pitch: Float,
        config: WallpaperConfig,
        palette: ColorPalette
    ) {
        val speed = config.animationSpeed
        backgroundPaint.color = palette.backgroundStart.toInt()
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), backgroundPaint)

        val slant = roll * 40f
        primaryPaint.color = palette.primaryColor.toInt()
        glowPaint.color = palette.glowColor.toInt()

        for (col in rainColumns) {
            col.y += (col.speed + pitch * 80f) * speed * dt
            if (col.y - col.length * 28f > h) {
                col.y = -20f
                col.speed = Random.nextFloat() * 250f + 160f
            }

            for (i in 0 until col.length) {
                val charY = col.y - (i * 24f)
                val charX = col.x + (charY / h) * slant

                if (charY in -10f..(h + 10f)) {
                    val alpha = if (i == 0) 255 else ((1f - i.toFloat() / col.length) * 190).toInt().coerceIn(10, 200)
                    primaryPaint.alpha = alpha
                    val glyphRadius = if (i == 0) 5f else 3f
                    canvas.drawCircle(charX, charY, glyphRadius, primaryPaint)

                    if (i == 0) {
                        glowPaint.alpha = 140
                        canvas.drawCircle(charX, charY, 12f, glowPaint)
                    }
                }
            }
        }
    }

    // 6. MYSTIC FIREFLIES ENGINE
    private fun renderFireflies(
        canvas: Canvas,
        w: Int,
        h: Int,
        time: Float,
        dt: Float,
        roll: Float,
        pitch: Float,
        config: WallpaperConfig,
        palette: ColorPalette
    ) {
        val speed = config.animationSpeed
        val bgShader = RadialGradient(
            w * 0.5f, h * 0.5f, h * 0.8f,
            intArrayOf(palette.backgroundEnd.toInt(), palette.backgroundStart.toInt()),
            null, Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), backgroundPaint)

        val activeCount = (particles.size * config.particleDensity * 0.4f).toInt().coerceIn(15, 42)

        for (i in 0 until activeCount) {
            val p = particles[i]
            // Gentle organic swaying
            val windX = roll * 60f + (sin(time * 0.8f * speed + p.pulseOffset) * 25f)
            val windY = pitch * 60f + (cos(time * 0.7f * speed + p.pulseOffset) * 20f)

            p.x += windX * dt * speed
            p.y += windY * dt * speed

            // Touch scatter
            for (tp in touchPoints) {
                if (tp.isActive) {
                    val dx = p.x - tp.x
                    val dy = p.y - tp.y
                    val dist = hypot(dx, dy).coerceAtLeast(1f)
                    if (dist < 260f) {
                        val push = (1f - dist / 260f) * 160f * speed
                        p.x += (dx / dist) * push * dt
                        p.y += (dy / dist) * push * dt
                    }
                }
            }

            if (p.x < 0) p.x = w.toFloat()
            if (p.x > w) p.x = 0f
            if (p.y < 0) p.y = h.toFloat()
            if (p.y > h) p.y = 0f

            // Firefly glow breathing cycle
            val breath = (sin(time * 2.5f * speed + p.pulseOffset) * 0.5f + 0.5f)
            val fireflyRadius = p.size * (0.8f + breath * 0.6f)

            // Outer soft aura
            glowPaint.color = palette.glowColor.toInt()
            glowPaint.alpha = (breath * 110).toInt()
            canvas.drawCircle(p.x, p.y, fireflyRadius * 4.5f, glowPaint)

            // Bright core
            primaryPaint.color = palette.primaryColor.toInt()
            primaryPaint.alpha = (150 + breath * 105).toInt()
            canvas.drawCircle(p.x, p.y, fireflyRadius, primaryPaint)
        }
    }

    private fun renderTouchRipples(canvas: Canvas, palette: ColorPalette) {
        linePaint.shader = null
        linePaint.color = palette.primaryColor.toInt()

        for (tp in touchPoints) {
            if (tp.rippleRadius > 0f && tp.rippleAlpha > 0f) {
                linePaint.strokeWidth = 3f * tp.rippleAlpha
                linePaint.alpha = (tp.rippleAlpha * 220).toInt()
                canvas.drawCircle(tp.x, tp.y, tp.rippleRadius, linePaint)
            }
        }
    }

    fun onTouchDown(index: Int, x: Float, y: Float) {
        if (index in touchPoints.indices) {
            val tp = touchPoints[index]
            tp.x = x
            tp.y = y
            tp.isActive = true
            tp.rippleRadius = 4f
            tp.rippleAlpha = 1f
        }
    }

    fun onTouchMove(index: Int, x: Float, y: Float) {
        if (index in touchPoints.indices) {
            val tp = touchPoints[index]
            tp.x = x
            tp.y = y
        }
    }

    fun onTouchUp(index: Int) {
        if (index in touchPoints.indices) {
            touchPoints[index].isActive = false
        }
    }
}
