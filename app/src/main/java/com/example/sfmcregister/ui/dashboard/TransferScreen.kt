package com.example.sfmcregister.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sfmcregister.ui.theme.OcbcRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onBackClick: () -> Unit
) {
    var nominal by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OcbcRed)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFE8EAF6)) // Light grey-blue background similar to reference
        ) {
            Text(
                text = "Tujuan",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = Color.Black
            )
            
            Column(
                modifier = Modifier.background(Color.Transparent)
            ) {
                TransferOptionItem(icon = Icons.Default.Person, label = "Antar rekening OCBC")
                Divider(color = Color.LightGray, thickness = 1.dp)
                TransferOptionItem(icon = Icons.Default.AccountBalance, label = "Bank lain")
                Divider(color = Color.LightGray, thickness = 1.dp)
                TransferOptionItem(icon = Icons.Default.QrCode, label = "QRIS")
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Nominal",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Black
            )
            
            OutlinedTextField(
                value = nominal,
                onValueChange = { nominal = it },
                placeholder = { Text("Rp 0") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            
            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = catatan,
                onValueChange = { catatan = it },
                placeholder = { Text("Catatan (opsional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            
            Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OcbcRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Lanjut", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun TransferOptionItem(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = OcbcRed, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, modifier = Modifier.weight(1f), color = Color.DarkGray)
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}
