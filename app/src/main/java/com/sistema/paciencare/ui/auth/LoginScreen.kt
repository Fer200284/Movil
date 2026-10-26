package com.sistema.paciencare.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sistema.paciencare.R
import com.sistema.paciencare.ui.components.CustomInput
import com.sistema.paciencare.ui.components.RoleItem

@Composable
fun LoginScreen(
    selectedRole: String,
    onRoleChange: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onLoginClick: (String, String) -> Unit,
    isError: Boolean = false,
    errorType: String? = null,
    isLoading: Boolean = false
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    var showLocalError by remember { mutableStateOf(false) }

    LaunchedEffect(isError) {
        if (isError) {
            showLocalError = true
        }
    }

    LaunchedEffect(email, password, selectedRole) {
        showLocalError = false
    }
    val colorPrimary = Color(0xFF0284C7)
    val colorSecondary = Color(0xFF0EA5E9)
    val colorBg = Color(0xFFF1F5F9)

    Box(modifier = Modifier.fillMaxSize().background(colorBg)) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(colorPrimary.copy(alpha = 0.1f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(Brush.verticalGradient(listOf(colorPrimary, colorSecondary)))
                    .statusBarsPadding()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        modifier = Modifier
                            .size(90.dp)
                            .shadow(12.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.paci),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "¡Hola! Bienvenido a",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "PacienCare",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Tu aliado en el cuidado de la salud",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Formulario Login
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "ACCESO COMO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RoleItem(
                        "Paciente",
                        Icons.Filled.AccountCircle,
                        selectedRole == "Paciente",
                        colorPrimary,
                        Modifier.weight(1f)
                    ) { onRoleChange("Paciente") }
                    RoleItem(
                        "Gerencia",
                        Icons.Filled.Home,
                        selectedRole == "Gerencia",
                        colorPrimary,
                        Modifier.weight(1f)
                    ) { onRoleChange("Gerencia") }
                }

                Spacer(modifier = Modifier.height(32.dp))

                CustomInput(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",
                    placeholder = "Ana@correo.com",
                    icon = Icons.Filled.Person,
                    accentColor = colorPrimary,
                    showError = showLocalError
                )
                Spacer(modifier = Modifier.height(20.dp))
                CustomInput(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    placeholder = "Ingresa tu Contraseña",
                    icon = Icons.Filled.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onVisibilityToggle = { passwordVisible = !passwordVisible },
                    accentColor = colorPrimary,
                    showError = showLocalError
                )

                TextButton(
                    onClick = onNavigateToForgot,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "¿Olvidaste tu contraseña?",
                        color = colorPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showLocalError) {
                    val errorMessage = when (errorType) {
                        "USUARIO_NO_EXISTE" -> "El usuario no se encuentra registrado"
                        "CONTRASENA_INCORRECTA" -> "La contraseña ingresada es incorrecta"
                        "ROL_INCORRECTO" -> "Este usuario no tiene permisos para el rol seleccionado"
                        "ROL_PACIENTE_NO_GERENCIA" -> "Tu cuenta está registrada como Paciente. Por favor selecciona el rol Paciente en el menú para ingresar."
                        "ROL_GERENCIA_NO_PACIENTE" -> "Tu cuenta está registrada como Gerencia. Por favor selecciona el rol Gerencia en el menú para ingresar."
                        else -> "Correo electrónico o contraseña incorrectos"
                    }
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        if (!isLoading) {
                            onLoginClick(email.trim().lowercase(), password.trim())
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(listOf(colorPrimary, Color(0xFF0369A1))),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            "Iniciar Sesión",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿No tienes una cuenta? ", 
                        color = Color(0xFF64748B),
                        fontSize = 14.sp)
                    
                    Text(
                        "Crear cuenta",
                        modifier = Modifier.clickable { onNavigateToRegister() },
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

