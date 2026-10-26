package com.sistema.paciencare.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sistema.paciencare.R
import com.sistema.paciencare.ui.components.CustomInput
import com.sistema.paciencare.ui.components.SuccessRegisterDialog
import com.sistema.paciencare.viewmodel.AuthViewModel
import com.sistema.paciencare.data.local.entities.UserEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(authViewModel: AuthViewModel, role: String, onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val colorPrimary = Color(0xFF0284C7)
    val colorAccent = Color(0xFF10B981)

    // Estados
    var nombre by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var fechaNac by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var authCode by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedSexo by remember { mutableStateOf("Seleccionar") }
    val sexos = listOf("Masculino", "Femenino", "Otros")

    var showErrors by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Lógica Calendario
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        fechaNac = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showSuccessDialog) {
        SuccessRegisterDialog(onConfirm = { showSuccessDialog = false; onBack() })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
            .navigationBarsPadding()
    ) {
        // 1. Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colorPrimary, Color(0xFF0EA5E9))))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
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
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Regresar",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Crear cuenta",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        if (role == "Gerencia") "Registro de personal de Gerencia" else "Completa tu información personal",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Card(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(4.dp, CircleShape),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
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

        // 2. Formulario según el Rol
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column {
                if (role == "Gerencia") {
                    // Formulario Simplificado Exclusivo para Gerencia
                    CustomInput(
                        value = nombre, onValueChange = { nombre = it; errorMessage = null },
                        label = "Nombre completo", placeholder = "Lic. Carlos Mendoza",
                        icon = Icons.Filled.Person, accentColor = colorPrimary,
                        isMandatory = true, showError = showErrors && nombre.isBlank()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomInput(
                                value = cedula, onValueChange = { cedula = it; errorMessage = null },
                                label = "Cédula", placeholder = "001-000000-0000X",
                                icon = Icons.Filled.CreditCard, accentColor = colorPrimary,
                                isMandatory = true, showError = showErrors && cedula.isBlank()
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomInput(
                                value = telefono, onValueChange = { telefono = it; errorMessage = null },
                                label = "Celular / Teléfono", placeholder = "+505 88888888",
                                icon = Icons.Filled.Phone, accentColor = colorPrimary,
                                isMandatory = true, showError = showErrors && telefono.isBlank()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomInput(
                        value = email, onValueChange = { email = it; errorMessage = null },
                        label = "Correo electrónico", placeholder = "gerencia@PacienCare.com",
                        icon = Icons.Filled.Email, accentColor = colorPrimary,
                        isMandatory = true, showError = showErrors && email.isBlank()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomInput(
                                value = password, onValueChange = { password = it; errorMessage = null },
                                label = "Contraseña", placeholder = "••••••••",
                                icon = Icons.Filled.Lock, isPassword = true, accentColor = colorPrimary,
                                isMandatory = true, showError = showErrors && (password.isBlank() || (confirmPassword.isNotBlank() && password.trim() != confirmPassword.trim()))
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomInput(
                                value = confirmPassword, onValueChange = { confirmPassword = it; errorMessage = null },
                                label = "Confirmar contraseña", placeholder = "••••••••",
                                icon = Icons.Filled.Lock, isPassword = true, accentColor = colorPrimary,
                                isMandatory = true, showError = showErrors && (confirmPassword.isBlank() || (password.isNotBlank() && password.trim() != confirmPassword.trim()))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFFAEB), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(12.dp)).padding(10.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Validación Administrativa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            CustomInput(
                                value = authCode, onValueChange = { authCode = it; errorMessage = null },
                                label = "Código de Autorización", placeholder = "Código secreto",
                                icon = Icons.Filled.Key, accentColor = Color(0xFFB45309),
                                isMandatory = true, showError = showErrors && (authCode.isBlank() || !authViewModel.validateAuthCode(authCode))
                            )
                        }
                    }
                } else {
                    // Formulario Completo para Paciente
                    CustomInput(
                        value = nombre, onValueChange = { nombre = it; errorMessage = null },
                        label = "Nombre completo", placeholder = "Ana García López",
                        icon = Icons.Filled.Person, accentColor = colorPrimary,
                        isMandatory = true, showError = showErrors && nombre.isBlank()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomInput(
                                value = cedula, onValueChange = { cedula = it },
                                label = "Cédula", placeholder = "123456789",
                                icon = Icons.Filled.CreditCard, accentColor = colorPrimary
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomInput(
                                value = fechaNac, onValueChange = { },
                                label = "Fecha de nacimiento", placeholder = "dd/mm/aaaa",
                                icon = Icons.Filled.DateRange, accentColor = colorPrimary,
                                readOnly = true, onClick = { showDatePicker = true }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sexo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                OutlinedTextField(
                                    value = selectedSexo, onValueChange = {}, readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).height(54.dp), shape = RoundedCornerShape(14.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = if (showErrors && selectedSexo == "Seleccionar") Color.Red else Color(0xFFE2E8F0),
                                        focusedBorderColor = colorPrimary
                                    )
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    sexos.forEach { item -> DropdownMenuItem(text = { Text(item, fontSize = 14.sp) }, onClick = { selectedSexo = item; expanded = false; errorMessage = null }) }
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomInput(value = telefono, onValueChange = { telefono = it }, label = "Teléfono", placeholder = "+505 58693313", icon = Icons.Filled.Phone, accentColor = colorPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomInput(
                        value = email, onValueChange = { email = it; errorMessage = null },
                        label = "Correo electrónico", placeholder = "correo@PacienCare.com",
                        icon = Icons.Filled.Email, accentColor = colorPrimary,
                        isMandatory = true, showError = showErrors && email.isBlank()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomInput(
                        value = direccion, onValueChange = { direccion = it; errorMessage = null },
                        label = "Dirección", placeholder = "Zona sur",
                        icon = Icons.Filled.LocationOn, accentColor = colorPrimary,
                        isMandatory = true, showError = showErrors && direccion.isBlank()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomInput(
                                value = password, onValueChange = { password = it; errorMessage = null },
                                label = "Contraseña", placeholder = "••••••••",
                                icon = Icons.Filled.Lock, isPassword = true, accentColor = colorPrimary,
                                isMandatory = true, showError = showErrors && (password.isBlank() || (confirmPassword.isNotBlank() && password.trim() != confirmPassword.trim()))
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomInput(
                                value = confirmPassword, onValueChange = { confirmPassword = it; errorMessage = null },
                                label = "Confirmar contraseña", placeholder = "••••••••",
                                icon = Icons.Filled.Lock, isPassword = true, accentColor = colorPrimary,
                                isMandatory = true, showError = showErrors && (confirmPassword.isBlank() || (password.isNotBlank() && password.trim() != confirmPassword.trim()))
                            )
                        }
                    }
                }
            }

            // 3. Acciones Finales
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        val cleanEmail = email.trim().lowercase()
                        val cleanPassword = password.trim()
                        val cleanConfirm = confirmPassword.trim()

                        if (role == "Gerencia") {
                            if (nombre.isBlank() || cedula.isBlank() || telefono.isBlank() ||
                                cleanEmail.isBlank() || cleanPassword.isBlank() || cleanConfirm.isBlank() || authCode.isBlank()) {
                                showErrors = true
                                errorMessage = "Por favor completa todos los campos obligatorios (*)"
                            } else if (cleanPassword != cleanConfirm) {
                                showErrors = true
                                errorMessage = "Las contraseñas no coinciden"
                            } else if (!authViewModel.validateAuthCode(authCode)) {
                                showErrors = true
                                errorMessage = "El código de autorización de gerencia es incorrecto"
                            } else {
                                showErrors = false
                                errorMessage = null
                                val newUser = UserEntity(
                                    email = cleanEmail,
                                    name = nombre.trim(),
                                    cedula = cedula.trim(),
                                    birthDate = "",
                                    gender = "No especificado",
                                    phone = telefono.trim(),
                                    address = "Gerencia",
                                    role = role,
                                    isFemale = false,
                                    password = cleanPassword
                                )
                                authViewModel.registerUser(
                                    user = newUser,
                                    authCode = authCode.trim(),
                                    onSuccess = {
                                        showSuccessDialog = true
                                    },
                                    onError = { err ->
                                        showErrors = true
                                        errorMessage = err
                                    }
                                )
                            }
                        } else {
                            if (nombre.isBlank() || cleanEmail.isBlank() || direccion.isBlank() || 
                                cleanPassword.isBlank() || cleanConfirm.isBlank()) {
                                showErrors = true
                                errorMessage = "Por favor completa todos los campos obligatorios (*)"
                            } else if (selectedSexo == "Seleccionar") {
                                showErrors = true
                                errorMessage = "Debes seleccionar tu Sexo"
                            } else if (cleanPassword != cleanConfirm) {
                                showErrors = true
                                errorMessage = "Las contraseñas no coinciden"
                            } else {
                                showErrors = false
                                errorMessage = null
                                val newUser = UserEntity(
                                    email = cleanEmail,
                                    name = nombre.trim(),
                                    cedula = cedula.trim(),
                                    birthDate = fechaNac.trim(),
                                    gender = selectedSexo,
                                    phone = telefono.trim(),
                                    address = direccion.trim(),
                                    role = role,
                                    isFemale = selectedSexo == "Femenino",
                                    password = cleanPassword
                                )
                                authViewModel.registerUser(
                                    user = newUser,
                                    onSuccess = {
                                        showSuccessDialog = true
                                    },
                                    onError = { err ->
                                        showErrors = true
                                        errorMessage = err
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).shadow(6.dp, RoundedCornerShape(14.dp)).background(colorAccent, RoundedCornerShape(14.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Crear cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onBack() },
                    modifier = Modifier.fillMaxWidth().height(52.dp).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
