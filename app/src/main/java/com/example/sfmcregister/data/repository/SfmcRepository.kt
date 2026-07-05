package com.example.sfmcregister.data.repository

import com.example.sfmcregister.data.model.RegisterForm
import com.example.sfmcregister.util.SfmcResult

/** Kontrak operasi Marketing Cloud. */
interface SfmcRepository {

    /**
     * Generate/reuse Contact Key (UUID), simpan ke SharedPreferences, lalu
     * daftarkan Contact Key + profile attributes ke Marketing Cloud.
     *
     * @return Contact Key (UUID) bila sukses.
     */
    suspend fun registerContact(form: RegisterForm): SfmcResult<String>
}
