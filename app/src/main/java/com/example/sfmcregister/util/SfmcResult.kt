package com.example.sfmcregister.util

/** Wrapper hasil operasi SDK agar error handling eksplisit dan tanpa throw ke UI. */
sealed interface SfmcResult<out T> {
    data class Success<T>(val data: T) : SfmcResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : SfmcResult<Nothing>
}
