package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DayType
import com.example.data.model.PricePoint
import com.example.data.repository.ElectricityRepository
import com.example.data.repository.ElectricityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ElectricityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ElectricityRepository.getInstance(application)

    val uiState: StateFlow<ElectricityUiState> = repository.uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.uiState.value
        )

    private val _isCarMode = MutableStateFlow(false)
    val isCarMode: StateFlow<Boolean> = _isCarMode.asStateFlow()

    private val _inspectedPricePoint = MutableStateFlow<PricePoint?>(null)
    val inspectedPricePoint: StateFlow<PricePoint?> = _inspectedPricePoint.asStateFlow()

    init {
        refresh()
    }

    fun selectDay(dayType: DayType) {
        repository.selectDay(dayType)
        _inspectedPricePoint.value = null
    }

    fun inspectPoint(point: PricePoint?) {
        _inspectedPricePoint.value = point
    }

    fun toggleCarMode() {
        _isCarMode.value = !_isCarMode.value
    }

    fun setCarMode(enabled: Boolean) {
        _isCarMode.value = enabled
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshPrices(force = true)
        }
    }
}
