package com.example.sfmcregister.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistered: (contactKey: String) -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Navigasi ke Success saat registrasi berhasil, lalu bersihkan state-nya
    // agar kembali ke layar ini tidak memicu navigasi ulang dengan key lama.
    LaunchedEffect(state.registeredContactKey) {
        state.registeredContactKey?.let { key ->
            viewModel.consumeRegistered()
            onRegistered(key)
        }
    }

    // Tampilkan error SDK/jaringan via Snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Register") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text("First Name") },
                isError = state.firstNameError != null,
                supportingText = { state.firstNameError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = { Text("Last Name") },
                isError = state.lastNameError != null,
                supportingText = { state.lastNameError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                isError = state.emailError != null,
                supportingText = { state.emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Opsional: pakai Contact Key tertentu (menyamakan identitas dengan
            // Individual yang ditarget flow/pesan di MC). Kosong = otomatis.
            OutlinedTextField(
                value = state.customContactKey,
                onValueChange = viewModel::onCustomContactKeyChange,
                label = { Text("Contact Key (opsional)") },
                supportingText = {
                    Text("Kosongkan untuk otomatis. Isi untuk menyamakan dengan Individual di MC.")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Mode manual (testing): SDK init + registrasi hanya saat Register ditekan
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mode Manual SDK", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Init SDK & registrasi hanya saat tombol Register ditekan. " +
                            "Berlaku penuh setelah app di-restart.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.manualMode,
                    onCheckedChange = viewModel::onManualModeChange
                )
            }

            Button(
                onClick = viewModel::register,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Register")
                }
            }

            // Info semua identitas SDK (party key, device id, token, attributes)
            OutlinedButton(
                onClick = viewModel::loadSdkInfo,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lihat Info SDK (party key, device id, dll)")
            }

            // TESTING: reset storage SDK → app tertutup → buka lagi = device ID baru
            OutlinedButton(
                onClick = viewModel::resetDeviceId,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Device ID (app akan tertutup)")
            }
        }

        // Dialog Info SDK — teks bisa diseleksi/di-copy
        state.sdkInfo?.let { info ->
            AlertDialog(
                onDismissRequest = viewModel::dismissSdkInfo,
                title = { Text("Info SDK") },
                text = {
                    SelectionContainer {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = info,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissSdkInfo) { Text("Tutup") }
                }
            )
        }
    }
}
