package com.example.sfmcregister.data.model

/** Data input registrasi dari UI. */
data class RegisterForm(
    val firstName: String,
    val lastName: String,
    val email: String,
    val age: String,
    val birthDate: String,
    val city: String,
    val gender: String,
    val occupation: String,
    val phone: String,
    val province: String,
    /** Opsional: pakai Contact Key tertentu (mis. menyamakan dengan Individual
     *  yang ditarget flow di MC). Kosong = otomatis (tersimpan/UUID baru). */
    val contactKeyOverride: String = ""
)
