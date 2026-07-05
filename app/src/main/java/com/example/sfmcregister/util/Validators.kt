package com.example.sfmcregister.util

import android.util.Patterns

/** Validasi input form registrasi. */
object Validators {

    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isNotBlank(value: String): Boolean = value.trim().isNotEmpty()
}
