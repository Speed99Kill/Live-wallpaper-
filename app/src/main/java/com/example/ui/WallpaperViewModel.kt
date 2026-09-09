package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.WallpaperPreferences
import com.example.data.WallpaperRepository
import com.example.model.ColorPalette
import com.example.model.TouchInteraction
import com.example.model.WallpaperConfig
import com.example.model.WallpaperEngineType
import com.example.sensors.MotionSensorTracker
import com.example.ui.components.WallpaperTarget
import com.example.ui.components.applyStaticWallpaper
import com.example.ui.components.renderWallpaperSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiNotification(val message: String)

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val preferences = WallpaperPreferences(application)
    val repository = WallpaperRepository(application, database, preferences)
    val motionTracker = MotionSensorTracker(application)

    private val _activeConfig = MutableStateFlow(repository.getActiveConfig())
    val activeConfig: StateFlow<WallpaperConfig> = _activeConfig.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _appTheme = MutableStateFlow(preferences.getAppTheme())
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _notifications = MutableSharedFlow<UiNotification>()
    val notifications: SharedFlow<UiNotification> = _notifications.asSharedFlow()

    val downloadedWallpapers: StateFlow<List<WallpaperConfig>> =
        repository.downloadedWallpapers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val catalogWallpapers: StateFlow<List<WallpaperConfig>> =
        combine(
            _selectedCategory,
            _searchQuery,
            repository.allCustomWallpapers
        ) { category, query, customWallpapers ->
            val all = (customWallpapers + repository.curatedPresets).distinctBy { it.id }
            all.filter { item ->
                val matchesCategory = (category == "All") || (item.engineType.category.equals(category, ignoreCase = true))
                val matchesQuery = query.isEmpty() ||
                        item.name.contains(query, ignoreCase = true) ||
                        item.engineType.title.contains(query, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.curatedPresets
        )

    init {
        motionTracker.configure(
            _activeConfig.value.sensorSensitivity,
            _activeConfig.value.invertMotion
        )
        motionTracker.startListening()
    }

    override fun onCleared() {
        super.onCleared()
        motionTracker.stopListening()
    }

    fun selectWallpaper(config: WallpaperConfig) {
        _activeConfig.value = config
        motionTracker.configure(config.sensorSensitivity, config.invertMotion)
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAppTheme(theme: String) {
        preferences.setAppTheme(theme)
        _appTheme.value = theme
    }

    fun createNewWallpaper() {
        val newConfig = WallpaperConfig(
            id = "custom_${System.currentTimeMillis()}",
            name = "My Custom Wallpaper",
            engineType = WallpaperEngineType.COSMIC_GRAVITY,
            animationSpeed = 1.0f,
            sensorSensitivity = 1.0f,
            invertMotion = false,
            particleDensity = 1.0f,
            paletteId = "cosmic_violet",
            touchInteraction = TouchInteraction.RIPPLE,
            isCustom = true
        )
        selectWallpaper(newConfig)
    }

    fun updateAnimationSpeed(speed: Float) {
        val updated = _activeConfig.value.copy(animationSpeed = speed.coerceIn(0.2f, 3.0f))
        _activeConfig.value = updated
        repository.setActiveConfig(updated)
    }

    fun updateSensorSensitivity(sensitivity: Float) {
        val updated = _activeConfig.value.copy(sensorSensitivity = sensitivity.coerceIn(0.0f, 3.0f))
        _activeConfig.value = updated
        motionTracker.configure(sensitivity, updated.invertMotion)
        repository.setActiveConfig(updated)
    }

    fun toggleInvertMotion() {
        val newInvert = !_activeConfig.value.invertMotion
        val updated = _activeConfig.value.copy(invertMotion = newInvert)
        _activeConfig.value = updated
        motionTracker.configure(updated.sensorSensitivity, newInvert)
        repository.setActiveConfig(updated)
    }

    fun updateParticleDensity(density: Float) {
        val updated = _activeConfig.value.copy(particleDensity = density.coerceIn(0.3f, 2.0f))
        _activeConfig.value = updated
        repository.setActiveConfig(updated)
    }

    fun selectPalette(paletteId: String) {
        val updated = _activeConfig.value.copy(paletteId = paletteId)
        _activeConfig.value = updated
        repository.setActiveConfig(updated)
    }

    fun selectTouchInteraction(interaction: TouchInteraction) {
        val updated = _activeConfig.value.copy(touchInteraction = interaction)
        _activeConfig.value = updated
        repository.setActiveConfig(updated)
    }

    fun downloadCurrentWallpaper() {
        viewModelScope.launch {
            val current = _activeConfig.value
            repository.downloadPreset(current)
            _notifications.emit(UiNotification("'${current.name}' downloaded to your collection!"))
        }
    }

    fun saveAsCustomPreset(name: String) {
        viewModelScope.launch {
            val custom = _activeConfig.value.copy(name = name.ifBlank { "Custom Wallpaper" })
            val newId = repository.saveCustomWallpaper(custom)
            _activeConfig.value = custom.copy(id = newId, isCustom = true, isDownloaded = true)
            _notifications.emit(UiNotification("Custom preset '$name' saved!"))
        }
    }

    fun downloadToDeviceGallery() {
        viewModelScope.launch {
            val current = _activeConfig.value
            val motion = motionTracker.motionFlow.value
            val snapshot = renderWallpaperSnapshot(
                config = current,
                roll = motion.roll,
                pitch = motion.pitch
            )
            val result = repository.exportBitmapToGallery(snapshot, current.name)
            result.onSuccess {
                _notifications.emit(UiNotification("4K Wallpaper saved to Gallery (Pictures/MotionPaper)!"))
            }.onFailure { err ->
                _notifications.emit(UiNotification("Failed to export: ${err.message}"))
            }
        }
    }

    fun applyStaticToDevice(target: WallpaperTarget) {
        viewModelScope.launch {
            val current = _activeConfig.value
            val motion = motionTracker.motionFlow.value
            val snapshot = renderWallpaperSnapshot(
                config = current,
                roll = motion.roll,
                pitch = motion.pitch
            )
            val success = applyStaticWallpaper(getApplication(), snapshot, target)
            val targetName = when (target) {
                WallpaperTarget.HOME -> "Home Screen"
                WallpaperTarget.LOCK -> "Lock Screen"
                WallpaperTarget.BOTH -> "Home & Lock Screen"
            }
            if (success) {
                _notifications.emit(UiNotification("Applied snapshot to $targetName!"))
            } else {
                _notifications.emit(UiNotification("Failed to set wallpaper"))
            }
        }
    }

    fun toggleFavorite(config: WallpaperConfig) {
        viewModelScope.launch {
            repository.toggleFavorite(config.id, config.isFavorite)
        }
    }

    fun deleteWallpaper(id: String) {
        viewModelScope.launch {
            repository.deleteSaved(id)
            _notifications.emit(UiNotification("Removed from collection"))
        }
    }

    fun setAsActiveLiveWallpaper() {
        repository.setActiveConfig(_activeConfig.value)
    }
}
