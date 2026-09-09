package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.ColorPalette
import com.example.model.TouchInteraction
import com.example.model.WallpaperConfig
import com.example.model.WallpaperEngineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class WallpaperRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val preferences: WallpaperPreferences
) {
    private val dao = database.wallpaperDao()

    val downloadedWallpapers: Flow<List<WallpaperConfig>> =
        dao.getDownloadedWallpapers().map { list -> list.map { it.toDomain() } }

    val allCustomWallpapers: Flow<List<WallpaperConfig>> =
        dao.getAllWallpapers().map { list -> list.map { it.toDomain() } }

    val curatedPresets: List<WallpaperConfig> = listOf(
        WallpaperConfig(
            id = "preset_cosmic_odyssey",
            name = "Cosmic Odyssey",
            engineType = WallpaperEngineType.COSMIC_GRAVITY,
            animationSpeed = 1.0f,
            sensorSensitivity = 1.4f,
            particleDensity = 1.1f,
            paletteId = "cosmic_violet",
            touchInteraction = TouchInteraction.ATTRACT
        ),
        WallpaperConfig(
            id = "preset_synthwave_84",
            name = "Synthwave 1984",
            engineType = WallpaperEngineType.NEON_SYNTHWAVE,
            animationSpeed = 1.2f,
            sensorSensitivity = 1.5f,
            particleDensity = 1.0f,
            paletteId = "retro_sunset",
            touchInteraction = TouchInteraction.RIPPLE
        ),
        WallpaperConfig(
            id = "preset_aurora_borealis",
            name = "Aurora Borealis",
            engineType = WallpaperEngineType.AURORA_LIQUID,
            animationSpeed = 0.8f,
            sensorSensitivity = 1.2f,
            particleDensity = 1.0f,
            paletteId = "emerald_aurora",
            touchInteraction = TouchInteraction.RIPPLE
        ),
        WallpaperConfig(
            id = "preset_neon_matrix",
            name = "Kinetic Neural Web",
            engineType = WallpaperEngineType.KINETIC_MATRIX,
            animationSpeed = 1.1f,
            sensorSensitivity = 1.6f,
            particleDensity = 1.2f,
            paletteId = "cyber_neon",
            touchInteraction = TouchInteraction.ATTRACT
        ),
        WallpaperConfig(
            id = "preset_cyberpunk_rain",
            name = "Neo-Tokyo Rain",
            engineType = WallpaperEngineType.DIGITAL_RAIN,
            animationSpeed = 1.3f,
            sensorSensitivity = 1.3f,
            particleDensity = 1.0f,
            paletteId = "cyber_neon",
            touchInteraction = TouchInteraction.SPARKLE
        ),
        WallpaperConfig(
            id = "preset_mystic_grove",
            name = "Mystic Fireflies",
            engineType = WallpaperEngineType.MYSTIC_FIREFLIES,
            animationSpeed = 0.7f,
            sensorSensitivity = 1.1f,
            particleDensity = 1.0f,
            paletteId = "deep_ocean",
            touchInteraction = TouchInteraction.REPEL
        ),
        WallpaperConfig(
            id = "preset_solar_cyclone",
            name = "Solar Cyclone",
            engineType = WallpaperEngineType.COSMIC_GRAVITY,
            animationSpeed = 1.5f,
            sensorSensitivity = 1.8f,
            particleDensity = 1.4f,
            paletteId = "solar_flare",
            touchInteraction = TouchInteraction.ATTRACT
        ),
        WallpaperConfig(
            id = "preset_electric_abyss",
            name = "Electric Abyss",
            engineType = WallpaperEngineType.AURORA_LIQUID,
            animationSpeed = 1.0f,
            sensorSensitivity = 1.3f,
            particleDensity = 1.0f,
            paletteId = "deep_ocean",
            touchInteraction = TouchInteraction.RIPPLE
        ),
        WallpaperConfig(
            id = "preset_quantum_core",
            name = "Quantum Constellation",
            engineType = WallpaperEngineType.KINETIC_MATRIX,
            animationSpeed = 0.9f,
            sensorSensitivity = 1.2f,
            particleDensity = 1.0f,
            paletteId = "cosmic_violet",
            touchInteraction = TouchInteraction.REPEL
        )
    )

    fun getActiveConfig(): WallpaperConfig = preferences.getActiveWallpaper()

    fun setActiveConfig(config: WallpaperConfig) {
        preferences.saveActiveWallpaper(config)
    }

    suspend fun downloadPreset(config: WallpaperConfig) = withContext(Dispatchers.IO) {
        val downloadedConfig = config.copy(
            isDownloaded = true,
            createdAt = System.currentTimeMillis()
        )
        dao.insertWallpaper(WallpaperEntity.fromDomain(downloadedConfig))
    }

    suspend fun saveCustomWallpaper(config: WallpaperConfig): String = withContext(Dispatchers.IO) {
        val newId = if (config.isCustom) config.id else "custom_${System.currentTimeMillis()}"
        val customConfig = config.copy(
            id = newId,
            isCustom = true,
            isDownloaded = true,
            createdAt = System.currentTimeMillis()
        )
        dao.insertWallpaper(WallpaperEntity.fromDomain(customConfig))
        newId
    }

    suspend fun toggleFavorite(id: String, currentFavorite: Boolean) = withContext(Dispatchers.IO) {
        val existing = dao.getWallpaperById(id)
        if (existing != null) {
            dao.updateFavoriteStatus(id, !currentFavorite)
        } else {
            // If it's a curated preset, insert it with favorite=true
            val preset = curatedPresets.firstOrNull { it.id == id }
            if (preset != null) {
                dao.insertWallpaper(
                    WallpaperEntity.fromDomain(preset.copy(isFavorite = true, isDownloaded = true))
                )
            }
        }
    }

    suspend fun deleteSaved(id: String) = withContext(Dispatchers.IO) {
        dao.deleteWallpaperById(id)
    }

    /**
     * Exports a rendered bitmap to the device Gallery / Pictures folder.
     */
    suspend fun exportBitmapToGallery(bitmap: Bitmap, title: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = "MotionPaper_${title.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            var outputStream: OutputStream? = null
            var savedPath = ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MotionPaper")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: throw IllegalStateException("Failed to create MediaStore entry")

                outputStream = context.contentResolver.openOutputStream(uri)
                savedPath = uri.toString()
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "MotionPaper").apply { mkdirs() }
                val imageFile = File(appDir, fileName)
                outputStream = FileOutputStream(imageFile)
                savedPath = imageFile.absolutePath
            }

            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            } ?: throw IllegalStateException("Failed to open output stream")

            savedPath
        }
    }
}
