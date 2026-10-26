package com.sistema.paciencare.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sistema.paciencare.R
import com.sistema.paciencare.ui.components.CustomInput

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val colorPrimary = Color(0xFF0284C7)
    val colorAccent = Color(0xFF10B981)
    var email by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colorPrimary, Color(0xFF0EA5E9))))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 26.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            null, tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Regresar",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Recuperar contraseña",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Te enviaremos las instrucciones a tu correo",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Card(
                    modifier = Modifier.size(54.dp).shadow(4.dp, CircleShape),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.paci),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Ingresa el correo electrónico asociado a tu cuenta para recibir un enlace de restablecimiento.",
                fontSize = 15.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            CustomInput(
                value = email,
                onValueChange = { email = it },
                label = "Correo electrónico",
                placeholder = "ejemplo@correo.com",
                icon = Icons.Filled.Person,
                accentColor = colorPrimary,
                isMandatory = true,
                showError = showErrors && email.isEmpty()
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { if (email.isEmpty()) showErrors = true else { /* Lógica enviar */ } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp))
                    .background(colorAccent, RoundedCornerShape(14.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Enviar instrucciones",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp,
                    Color(0xFFE2E8F0))
            ) {
                Text(
                    "Cancelar",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
