package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * GymViewModel exposes clean state flows for the sport timer, workout logging, meals engine, 
 * and real-time Voice Coach streaming states.
 * 
 * Entirely clean, concise, and relies completely on the high-level standard Live Session states.
 */
class GymViewModel(application: Application) : AndroidViewModel(application) {
    private val gymDao = GymDatabase.getDatabase(application).gymDao()
    val aiAssistant = AIAssistant(application)
    
    // Voice Coach Pure Audio Streaming States (Subscribing directly to Service global flows)
    val isListening: StateFlow<Boolean> = VoiceCoachService.isLiveConnected
    val isSpeaking: StateFlow<Boolean> = VoiceCoachService.isSpeaking
    val voiceWaveformAmplitudes: StateFlow<List<Float>> = VoiceCoachService.amplitudes

    // --- Core Gym Performance & Nutrition States ---
    
    private val _targetTab = MutableStateFlow<DashboardTab?>(null)
    val targetTab = _targetTab.asStateFlow()

    fun requestTabChange(tab: DashboardTab) {
        _targetTab.value = tab
    }

    fun clearTabChangeRequest() {
        _targetTab.value = null
    }

    val macroProgress: StateFlow<MacroProgress> = gymDao.getMacros()
        .map { it ?: MacroProgress() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MacroProgress()
        )

    val workoutProgress: StateFlow<List<WorkoutProgress>> = gymDao.getAllWorkouts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateMacros(macros: MacroProgress) {
        viewModelScope.launch {
            gymDao.updateMacros(macros)
        }
    }

    fun updateWorkout(progress: WorkoutProgress) {
        viewModelScope.launch {
            gymDao.updateWorkout(progress)
        }
    }

    fun resetWorkouts() {
        viewModelScope.launch {
            gymDao.resetWorkouts()
        }
    }
}
