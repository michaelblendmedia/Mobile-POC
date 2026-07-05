package com.example.sfmcregister.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sfmcregister.data.local.ContactPreferences
import com.example.sfmcregister.data.model.RegisterForm
import com.example.sfmcregister.data.repository.SfmcRepository
import com.example.sfmcregister.data.sfmc.SfmcInitializer
import com.example.sfmcregister.util.SfmcResult
import com.example.sfmcregister.util.Validators
import com.salesforce.marketingcloud.pushfeature.PushFeature
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: SfmcRepository,
    private val prefs: ContactPreferences,
    private val initializer: SfmcInitializer
) : ViewModel() {

    /** TESTING: hapus storage SDK & tutup app → device ID baru saat dibuka lagi. */
    fun resetDeviceId() = initializer.resetDeviceIdentityAndExit()

    /** Kumpulkan semua identitas dari SDK untuk ditampilkan di dialog Info. */
    fun loadSdkInfo() {
        _uiState.update {
            it.copy(sdkInfo = "Memuat info SDK…\n(mode manual: init dulu lewat Register)")
        }
        SFMCSdk.requestSdk { sdk ->
            val id = sdk.identity
            val base = buildString {
                appendLine("── IDENTITAS USER ──")
                appendLine("Party / Contact Key:")
                appendLine("${id.partyIdentificationNumber ?: "-"}")
                appendLine("Party Name : ${id.partyIdentificationName ?: "-"}")
                appendLine("Party Type : ${id.partyIdentificationType ?: "-"}")
                appendLine("Profile ID :")
                appendLine("${id.profileId ?: "-"}")
                appendLine("Contact Key tersimpan (prefs):")
                appendLine("${prefs.contactKey ?: "-"}")
                appendLine()
                appendLine("── DEVICE ──")
                appendLine("Device / Registration ID:")
                appendLine(id.registrationId)
                appendLine()
                appendLine("── ATTRIBUTES ──")
                if (id.attributes.isEmpty()) appendLine("(kosong)")
                id.attributes.forEach { (k, v) -> appendLine("$k = $v") }
            }
            _uiState.update { it.copy(sdkInfo = base) }

            // Info push menyusul (callback terpisah)
            PushFeature.requestSdk { pf ->
                val pmm = pf.getPushMessageManager()
                val full = base + buildString {
                    appendLine()
                    appendLine("── PUSH ──")
                    appendLine("Push enabled: ${pmm.isPushEnabled()}")
                    appendLine("Push token:")
                    appendLine(pmm.getPushToken() ?: "-")
                }
                _uiState.update { it.copy(sdkInfo = full) }
            }
        }
    }

    fun dismissSdkInfo() = _uiState.update { it.copy(sdkInfo = null) }

    private val _uiState = MutableStateFlow(RegisterUiState(manualMode = prefs.manualInitMode))
    val uiState = _uiState.asStateFlow()

    /** Toggle mode manual (SDK init saat Register). Berlaku penuh setelah app restart. */
    fun onManualModeChange(enabled: Boolean) {
        prefs.manualInitMode = enabled
        _uiState.update { it.copy(manualMode = enabled) }
    }

    fun onFirstNameChange(v: String) = _uiState.update { it.copy(firstName = v, firstNameError = null) }
    fun onLastNameChange(v: String) = _uiState.update { it.copy(lastName = v, lastNameError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null) }
    fun onCustomContactKeyChange(v: String) = _uiState.update { it.copy(customContactKey = v) }
    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    /** Dipanggil setelah navigasi ke Success — bersihkan key agar register
     *  berikutnya tidak menampilkan/menavigasi ulang dengan key lama. */
    fun consumeRegistered() = _uiState.update { it.copy(registeredContactKey = null) }

    fun register() {
        val s = _uiState.value

        // Validasi
        val firstErr = if (!Validators.isNotBlank(s.firstName)) "First name wajib diisi" else null
        val lastErr = if (!Validators.isNotBlank(s.lastName)) "Last name wajib diisi" else null
        val emailErr = when {
            !Validators.isNotBlank(s.email) -> "Email wajib diisi"
            !Validators.isValidEmail(s.email) -> "Format email tidak valid"
            else -> null
        }

        if (firstErr != null || lastErr != null || emailErr != null) {
            _uiState.update {
                it.copy(firstNameError = firstErr, lastNameError = lastErr, emailError = emailErr)
            }
            return
        }

        // Valid → proses ke Marketing Cloud
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = repository.registerContact(
                RegisterForm(
                    firstName = s.firstName,
                    lastName = s.lastName,
                    email = s.email,
                    contactKeyOverride = s.customContactKey
                )
            )

            _uiState.update {
                when (result) {
                    is SfmcResult.Success ->
                        it.copy(isLoading = false, registeredContactKey = result.data)
                    is SfmcResult.Error ->
                        it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
