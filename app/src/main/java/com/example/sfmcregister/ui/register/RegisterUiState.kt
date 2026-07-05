package com.example.sfmcregister.ui.register

/** Single source of truth untuk state layar Register. */
data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val customContactKey: String = "",       // Opsional: pakai Contact Key tertentu
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val isLoading: Boolean = false,          // Loading state
    val errorMessage: String? = null,        // Error dari SDK/jaringan (untuk Snackbar)
    val registeredContactKey: String? = null, // != null => sukses, siap navigasi ke Success
    val manualMode: Boolean = false,         // Mode manual: SDK init saat Register (testing)
    val sdkInfo: String? = null              // != null => tampilkan dialog Info SDK
)
