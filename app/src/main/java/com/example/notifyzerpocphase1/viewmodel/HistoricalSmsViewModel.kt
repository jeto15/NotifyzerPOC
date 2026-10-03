package com.example.notifyzerpocphase1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notifyzerpocphase1.model.HistoricalSms
import com.example.notifyzerpocphase1.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoricalSmsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)

    private val _groupedSms = MutableStateFlow<Map<String, List<HistoricalSms>>>(emptyMap())
    val groupedSms: StateFlow<Map<String, List<HistoricalSms>>> = _groupedSms.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
        if (granted) {
            loadSms()
        }
    }

    fun loadSms() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _groupedSms.value = repository.getGroupedSms()
            } catch (e: Exception) {
                // handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
