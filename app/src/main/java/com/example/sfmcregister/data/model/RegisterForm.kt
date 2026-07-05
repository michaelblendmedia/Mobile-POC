package com.example.sfmcregister.data.model

/** Data input registrasi dari UI. */
data class RegisterForm(
    val firstName: String,
    val lastName: String,
    val email: String,
    /** Opsional: pakai Contact Key tertentu (mis. menyamakan dengan Individual
     *  yang ditarget flow di MC). Kosong = otomatis (tersimpan/UUID baru). */
    val contactKeyOverride: String = ""
)
