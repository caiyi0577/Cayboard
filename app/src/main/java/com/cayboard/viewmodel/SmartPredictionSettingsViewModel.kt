package com.cayboard.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cayboard.association.AssociationManager
import com.cayboard.settings.SettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SmartPredictionUiState(
    val isEnabled: Boolean = false,
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val toastMessage: String? = null
)

class SmartPredictionSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private var modelOperation: kotlinx.coroutines.Job? = null
    
    private val _uiState = MutableStateFlow(SmartPredictionUiState(
        isEnabled = SettingsPreferences.isSmartPredictionEnabled(context),
        isInitialized = AssociationManager.isInitialized()
    ))
    val uiState: StateFlow<SmartPredictionUiState> = _uiState.asStateFlow()
    
    fun setEnabled(enabled: Boolean) {
        SettingsPreferences.setSmartPredictionEnabled(context, enabled)
        _uiState.update { it.copy(isEnabled = enabled) }
        
        if (enabled && !_uiState.value.isInitialized) {
            loadModel()
        } else if (!enabled) {
            releaseModel()
        }
    }
    
    private fun loadModel() {
        modelOperation?.cancel()
        modelOperation = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val success = withContext(Dispatchers.IO) {
                AssociationManager.initialize(context)
            }
            
            _uiState.update { it.copy(
                isInitialized = success,
                isLoading = false
            )}
            
            if (!success) {
                _uiState.update { it.copy(toastMessage = "联想加载失败") }
            }
        }
    }
    
    private fun releaseModel() {
        modelOperation?.cancel()
        AssociationManager.release()
        _uiState.update { it.copy(isInitialized = false, isLoading = false) }
    }
    
    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
    
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
