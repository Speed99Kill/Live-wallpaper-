package com.example.model

import androidx.compose.ui.graphics.Color

enum class WallpaperEngineType(
    val title: String,
    val description: String,
    val category: String,
    val iconName: String
) {
    COSMIC_GRAVITY(
        title = "Cosmic Nebula",
        description = "Gravitational star clusters & cosmic dust responding to device tilt & gravitational finger pull.",
        category = "Cosmic",
        iconName = "auto_awesome"
    ),
    NEON_SYNTHWAVE(
        title = "Synthwave 3D Grid",
        description = "Retro 80s wireframe horizon with a pulsing neon sun. Gyroscope tilts perspective horizon.",
        category = "Cyberpunk",
        iconName = "grid_view"
    ),
    AURORA_LIQUID(
        title = "Liquid Aurora",
        description = "Fluid luminous ribbons oscillating harmonically. Tilt sloshes fluid waves with liquid ripples.",
        category = "Fluid",
        iconName = "waves"
    ),
    KINETIC_MATRIX(
        title = "Kinetic Constellation",
        description = "Floating geometric nodes connected by dynamic energy threads with real physical inertia.",
        category = "Geometric",
        iconName = "hub"
    ),
    DIGITAL_RAIN(
        title = "Cyber Cyberglyphs",
        description = "Futuristic cascading digital code streams that tilt with orientation and scatter on impact.",
        category = "Cyberpunk",
        iconName = "terminal"
    ),
    MYSTIC_FIREFLIES(
        title = "Bioluminescent Grove",
        description = "Soft organic glowing orbs floating in deep twilight, drifting with gentle breeze sensor response.",
        category = "Nature",
        iconName = "forest"
    )
}

enum class TouchInteraction(val label: String) {
    ATTRACT("Attract / Pull"),
    REPEL("Repel / Scatter"),
    RIPPLE("Liquid Ripple"),
    SPARKLE("Pulse Burst")
}

data class ColorPalette(
    val id: String,
    val name: String,
    val backgroundStart: Long,
    val backgroundEnd: Long,
    val primaryColor: Long,
    val secondaryColor: Long,
    val accentColor: Long,
    val glowColor: Long
) {
    fun toComposeBackgroundStart() = Color(backgroundStart)
    fun toComposeBackgroundEnd() = Color(backgroundEnd)
    fun toComposePrimary() = Color(primaryColor)
    fun toComposeSecondary() = Color(secondaryColor)
    fun toComposeAccent() = Color(accentColor)
    fun toComposeGlow() = Color(glowColor)

    companion object {
        val PALETTES = listOf(
            ColorPalette(
                id = "cyber_neon",
                name = "Cyber Neon",
                backgroundStart = 0xFF080915,
                backgroundEnd = 0xFF140D2B,
                primaryColor = 0xFF00E5FF,
                secondaryColor = 0xFFFF007F,
                accentColor = 0xFF7C4DFF,
                glowColor = 0x8000E5FF
            ),
            ColorPalette(
                id = "cosmic_violet",
                name = "Cosmic Violet",
                backgroundStart = 0xFF050512,
                backgroundEnd = 0xFF1B0F2F,
                primaryColor = 0xFF9D4EDD,
                secondaryColor = 0xFFFF54B0,
                accentColor = 0xFF00F5D4,
                glowColor = 0x809D4EDD
            ),
            ColorPalette(
                id = "emerald_aurora",
                name = "Emerald Aurora",
                backgroundStart = 0xFF021314,
                backgroundEnd = 0xFF052B24,
                primaryColor = 0xFF00F5A0,
                secondaryColor = 0xFF00D9F5,
                accentColor = 0xFF70FF00,
                glowColor = 0x8000F5A0
            ),
            ColorPalette(
                id = "solar_flare",
                name = "Solar Flare",
                backgroundStart = 0xFF170805,
                backgroundEnd = 0xFF2A1005,
                primaryColor = 0xFFFF5722,
                secondaryColor = 0xFFFFAB00,
                accentColor = 0xFFFF1744,
                glowColor = 0x80FF9100
            ),
            ColorPalette(
                id = "deep_ocean",
                name = "Deep Abyss",
                backgroundStart = 0xFF020B14,
                backgroundEnd = 0xFF081E38,
                primaryColor = 0xFF00B0FF,
                secondaryColor = 0xFF00E5FF,
                accentColor = 0xFF448AFF,
                glowColor = 0x8000B0FF
            ),
            ColorPalette(
                id = "retro_sunset",
                name = "Miami Sunset",
                backgroundStart = 0xFF12041D,
                backgroundEnd = 0xFF2B0A3D,
                primaryColor = 0xFFFF007F,
                secondaryColor = 0xFFFFB300,
                accentColor = 0xFF8A2BE2,
                glowColor = 0x80FF007F
            )
        )

        fun getById(id: String): ColorPalette {
            return PALETTES.firstOrNull { it.id == id } ?: PALETTES.first()
        }
    }
}

data class WallpaperConfig(
    val id: String,
    val name: String,
    val engineType: WallpaperEngineType,
    val animationSpeed: Float = 1.0f, // 0.2f to 3.0f
    val sensorSensitivity: Float = 1.2f, // 0.0f to 3.0f
    val invertMotion: Boolean = false,
    val particleDensity: Float = 1.0f, // 0.3f to 2.0f
    val paletteId: String = "cyber_neon",
    val touchInteraction: TouchInteraction = TouchInteraction.ATTRACT,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val palette: ColorPalette
        get() = ColorPalette.getById(paletteId)
}
