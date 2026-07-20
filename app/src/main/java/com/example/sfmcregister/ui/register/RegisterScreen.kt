package com.example.sfmcregister.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistered: (contactKey: String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.registeredContactKey) {
        state.registeredContactKey?.let { key ->
            viewModel.consumeRegistered()
            onRegistered(key)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    val backgroundColor = Color.White
    val contentColor = Color(0xFF1A1A2E)
    val primaryColor = MaterialTheme.colorScheme.primary
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = contentColor,
        unfocusedTextColor = contentColor,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        cursorColor = primaryColor,
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = contentColor.copy(alpha = 0.5f),
        focusedLabelColor = primaryColor,
        unfocusedLabelColor = contentColor.copy(alpha = 0.7f),
        errorTextColor = MaterialTheme.colorScheme.error,
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorSupportingTextColor = MaterialTheme.colorScheme.error,
        errorLabelColor = MaterialTheme.colorScheme.error
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(backgroundColor)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = contentColor,
                    modifier = Modifier.clickable { onBackClick() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Buat Akun",
                color = contentColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Daftar untuk mulai menggunakan aplikasi",
                color = contentColor,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            OutlinedTextField(
                value = state.customContactKey,
                onValueChange = viewModel::onCustomContactKeyChange,
                label = { Text("User ID") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text("Nama Depan") },
                isError = state.firstNameError != null,
                supportingText = { state.firstNameError?.let { Text(it) } },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = { Text("Nama Belakang") },
                isError = state.lastNameError != null,
                supportingText = { state.lastNameError?.let { Text(it) } },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                isError = state.emailError != null,
                supportingText = { state.emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Nomor Telepon (opsional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.age,
                onValueChange = viewModel::onAgeChange,
                label = { Text("Umur (opsional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.birthDate,
                onValueChange = viewModel::onBirthDateChange,
                label = { Text("Tanggal Lahir (opsional)") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.gender,
                onValueChange = viewModel::onGenderChange,
                label = { Text("Gender (opsional)") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.city,
                onValueChange = viewModel::onCityChange,
                label = { Text("Kota (opsional)") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.province,
                onValueChange = viewModel::onProvinceChange,
                label = { Text("Provinsi (opsional)") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.occupation,
                onValueChange = viewModel::onOccupationChange,
                label = { Text("Pekerjaan (opsional)") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = viewModel::register,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor.copy(alpha = if (state.isLoading) 0.5f else 1f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Daftar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Debug tools moved to bottom and styled transparently
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mode Manual SDK", style = MaterialTheme.typography.titleSmall, color = contentColor)
                }
                Switch(
                    checked = state.manualMode,
                    onCheckedChange = viewModel::onManualModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = primaryColor
                    )
                )
            }

            OutlinedButton(
                onClick = viewModel::loadSdkInfo,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
            ) {
                Text("Lihat Info SDK")
            }

            OutlinedButton(
                onClick = viewModel::resetDeviceId,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
            ) {
                Text("Reset Device ID")
            }
        }

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
