package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.TouchInteraction
import com.example.model.WallpaperConfig
import com.example.model.WallpaperEngineType

class WallpaperPreferences(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getActiveWallpaper(): WallpaperConfig {
        val id = prefs.getString(KEY_ID, "cosmic_odyssey") ?: "cosmic_odyssey"
        val name = prefs.getString(KEY_NAME, "Cosmic Odyssey") ?: "Cosmic Odyssey"
        val engineStr = prefs.getString(KEY_ENGINE, WallpaperEngineType.COSMIC_GRAVITY.name)
        val engineType = runCatching { WallpaperEngineType.valueOf(engineStr ?: "") }
            .getOrDefault(WallpaperEngineType.COSMIC_GRAVITY)
        val speed = prefs.getFloat(KEY_SPEED, 1.0f)
        val sensitivity = prefs.getFloat(KEY_SENSITIVITY, 1.2f)
        val invertMotion = prefs.getBoolean(KEY_INVERT, false)
        val density = prefs.getFloat(KEY_DENSITY, 1.0f)
        val paletteId = prefs.getString(KEY_PALETTE, "cosmic_violet") ?: "cosmic_violet"
        val touchStr = prefs.getString(KEY_TOUCH, TouchInteraction.ATTRACT.name)
        val touch = runCatching { TouchInteraction.valueOf(touchStr ?: "") }
            .getOrDefault(TouchInteraction.ATTRACT)

        return WallpaperConfig(
            id = id,
            name = name,
            engineType = engineType,
            animationSpeed = speed,
            sensorSensitivity = sensitivity,
            invertMotion = invertMotion,
            particleDensity = density,
            paletteId = paletteId,
            touchInteraction = touch,
            isDownloaded = true,
            isFavorite = true
        )
    }

    fun saveActiveWallpaper(config: WallpaperConfig) {
        prefs.edit()
            .putString(KEY_ID, config.id)
            .putString(KEY_NAME, config.name)
            .putString(KEY_ENGINE, config.engineType.name)
            .putFloat(KEY_SPEED, config.animationSpeed)
            .putFloat(KEY_SENSITIVITY, config.sensorSensitivity)
            .putBoolean(KEY_INVERT, config.invertMotion)
            .putFloat(KEY_DENSITY, config.particleDensity)
            .putString(KEY_PALETTE, config.paletteId)
            .putString(KEY_TOUCH, config.touchInteraction.name)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "motion_paper_active_prefs"
        private const val KEY_ID = "active_id"
        private const val KEY_NAME = "active_name"
        private const val KEY_ENGINE = "active_engine"
        private const val KEY_SPEED = "active_speed"
        private const val KEY_SENSITIVITY = "active_sensitivity"
        private const val KEY_INVERT = "active_invert"
        private const val KEY_DENSITY = "active_density"
        private const val KEY_PALETTE = "active_palette"
        private const val KEY_TOUCH = "active_touch"
        private const val KEY_UPDATED_AT = "active_updated_at"
    }
}
