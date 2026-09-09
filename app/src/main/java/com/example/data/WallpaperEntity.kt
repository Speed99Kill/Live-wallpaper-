package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.TouchInteraction
import com.example.model.WallpaperConfig
import com.example.model.WallpaperEngineType

@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val engineType: String,
    val animationSpeed: Float,
    val sensorSensitivity: Float,
    val invertMotion: Boolean,
    val particleDensity: Float,
    val paletteId: String,
    val touchInteraction: String,
    val isDownloaded: Boolean,
    val isFavorite: Boolean,
    val isCustom: Boolean,
    val createdAt: Long
) {
    fun toDomain(): WallpaperConfig {
        return WallpaperConfig(
            id = id,
            name = name,
            engineType = runCatching { WallpaperEngineType.valueOf(engineType) }
                .getOrDefault(WallpaperEngineType.COSMIC_GRAVITY),
            animationSpeed = animationSpeed,
            sensorSensitivity = sensorSensitivity,
            invertMotion = invertMotion,
            particleDensity = particleDensity,
            paletteId = paletteId,
            touchInteraction = runCatching { TouchInteraction.valueOf(touchInteraction) }
                .getOrDefault(TouchInteraction.ATTRACT),
            isDownloaded = isDownloaded,
            isFavorite = isFavorite,
            isCustom = isCustom,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(config: WallpaperConfig): WallpaperEntity {
            return WallpaperEntity(
                id = config.id,
                name = config.name,
                engineType = config.engineType.name,
                animationSpeed = config.animationSpeed,
                sensorSensitivity = config.sensorSensitivity,
                invertMotion = config.invertMotion,
                particleDensity = config.particleDensity,
                paletteId = config.paletteId,
                touchInteraction = config.touchInteraction.name,
                isDownloaded = config.isDownloaded,
                isFavorite = config.isFavorite,
                isCustom = config.isCustom,
                createdAt = config.createdAt
            )
        }
    }
}
