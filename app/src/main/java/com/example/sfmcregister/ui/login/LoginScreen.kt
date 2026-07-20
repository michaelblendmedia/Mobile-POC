package com.example.sfmcregister.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sfmcregister.ui.dashboard.DashboardViewModel
import com.example.sfmcregister.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Use White as background
    val backgroundColor = Color.White
    val contentColor = Color(0xFF1A1A2E)
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        // OCBC Logo
        Image(
            painter = painterResource(id = R.drawable.ocbc_logo),
            contentDescription = "OCBC Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "OCBC Mobile",
            color = contentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selamat datang",
            color = contentColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Masuk untuk melanjutkan",
            color = contentColor,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Input Fields
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = contentColor,
            unfocusedTextColor = contentColor,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = primaryColor,
            focusedBorderColor = primaryColor,
            unfocusedBorderColor = contentColor.copy(alpha = 0.5f),
            focusedLabelColor = primaryColor,
            unfocusedLabelColor = contentColor.copy(alpha = 0.7f)
        )

        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            label = { Text("User ID") },
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (userId.isNotBlank()) {
                    viewModel.setFirstName(userId)
                }
                viewModel.trackEvent("User_Login", mapOf("UserId" to userId))
                onLoginClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Masuk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.clickable { /* Handle biometric */ }
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Biometric",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Masuk dengan biometrik",
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Text(text = "Belum punya akun? ", color = contentColor)
            Text(
                text = "Daftar",
                color = primaryColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }
    }
}
