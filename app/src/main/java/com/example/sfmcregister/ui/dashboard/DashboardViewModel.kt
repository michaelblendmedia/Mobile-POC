package com.example.sfmcregister.ui.dashboard

import androidx.lifecycle.ViewModel
import com.example.sfmcregister.data.local.ContactPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import com.example.sfmcregister.data.repository.SfmcRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val prefs: ContactPreferences,
    private val repository: SfmcRepository
) : ViewModel() {
    val firstName: String
        get() = prefs.firstName ?: "Pelanggan"
    
    fun setFirstName(name: String) {
        prefs.firstName = name
    }

    fun trackEvent(name: String, attributes: Map<String, Any> = emptyMap()) {
        viewModelScope.launch {
            repository.trackEvent(name, attributes)
        }
    }
}
