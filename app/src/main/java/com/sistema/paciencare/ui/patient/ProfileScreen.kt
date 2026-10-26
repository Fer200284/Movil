package com.sistema.paciencare.ui.patient

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.sistema.paciencare.data.local.SessionManager
import com.sistema.paciencare.R
import com.sistema.paciencare.viewmodel.PatientViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(patientViewModel: PatientViewModel, onBack: () -> Unit, onLogout: () -> Unit) {
    val colorPrimary = Color(0xFF0284C7)
    val colorBg = Color(0xFFF1F5F9)
    val scrollState = rememberScrollState()

    val user by patientViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }

    // Estados de los campos vinculados al usuario de la BD
    var nombreCompleto by remember(user) { mutableStateOf(user?.name ?: "") }
    var cedula by remember(user) { mutableStateOf(user?.cedula ?: "") }
    var fechaNac by remember(user) { mutableStateOf(user?.birthDate ?: "") }
    var sexo by remember(user) { mutableStateOf(user?.gender ?: "") }
    var telefono by remember(user) { mutableStateOf(user?.phone ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
    var direccion by remember(user) { mutableStateOf(user?.address ?: "") }

    // Estados de edición
    var editNombre by remember { mutableStateOf(false) }
    var editCedula by remember { mutableStateOf(false) }
    var editFechaNac by remember { mutableStateOf(false) }
    var editSexo by remember { mutableStateOf(false) }
    var editTelefono by remember { mutableStateOf(false) }
    var editDireccion by remember { mutableStateOf(false) }

    // Actualización reactiva cuando se reciben los datos remotos de la API
    LaunchedEffect(user) {
        if (user != null) {
            if (!editNombre) nombreCompleto = user?.name ?: ""
            if (!editCedula) cedula = user?.cedula ?: ""
            if (!editFechaNac) fechaNac = user?.birthDate ?: ""
            if (!editSexo) sexo = user?.gender ?: ""
            if (!editTelefono) telefono = user?.phone ?: ""
            email = user?.email ?: ""
            if (!editDireccion) direccion = user?.address ?: ""
        }
    }

    // Estado Diálogo Logout
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("¿Cerrar sesión?", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas salir de tu cuenta?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Cerrar sesión", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                patientViewModel.refreshData()
                delay(800)
                isRefreshing = false
            }
        },
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
            )
        },
        modifier = Modifier.fillMaxSize().background(colorBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // 1. Header con LOGO PACI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(colorPrimary, Color(0xFF0EA5E9))))
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onBack() }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White)
                            Text("Regresar", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                        Card(
                            modifier = Modifier.size(46.dp).shadow(4.dp, CircleShape),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.paci),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Mi perfil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Información personal", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            // 2. Cuerpo Compacto (Zero-Scroll)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar más pequeño
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, colorPrimary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = if ((user?.name ?: "").length >= 2) user?.name?.take(2)?.uppercase() ?: "U" else "U"
                    Text(initials, fontSize = 24.sp, fontWeight = FontWeight.Black, color = colorPrimary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(user?.name ?: "Usuario", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text("Paciente registrado", fontSize = 12.sp, color = Color(0xFF64748B))

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta de Información
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        EditableProfileField("NOMBRE COMPLETO", nombreCompleto, editNombre, { nombreCompleto = it }, { editNombre = !editNombre }, colorPrimary)
                        ProfileDivider()
                        EditableProfileField("CÉDULA", cedula, editCedula, { cedula = it }, { editCedula = !editCedula }, colorPrimary)
                        ProfileDivider()
                        EditableProfileField("FECHA DE NACIMIENTO", fechaNac, editFechaNac, { fechaNac = it }, { editFechaNac = !editFechaNac }, colorPrimary)
                        ProfileDivider()
                        EditableProfileField("SEXO", sexo, editSexo, { sexo = it }, { editSexo = !editSexo }, colorPrimary)
                        ProfileDivider()
                        EditableProfileField("TELÉFONO", telefono, editTelefono, { telefono = it }, { editTelefono = !editTelefono }, colorPrimary)
                        ProfileDivider()
                        EditableProfileField("CORREO ELECTRÓNICO", email, false, {}, {}, colorPrimary, canEdit = false)
                        ProfileDivider()
                        EditableProfileField("DIRECCIÓN", direccion, editDireccion, { direccion = it }, { editDireccion = !editDireccion }, colorPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones de Acción
                Button(
                    onClick = {
                        user?.let { current ->
                            val isFemaleValue = sexo.equals("Femenino", ignoreCase = true)
                            val finalName = if (nombreCompleto.isNotBlank()) nombreCompleto.trim() else current.name
                            val updatedUser = current.copy(
                                name = finalName,
                                cedula = cedula,
                                birthDate = fechaNac,
                                gender = sexo,
                                phone = telefono,
                                address = direccion,
                                isFemale = isFemaleValue
                            )
                            patientViewModel.updateUser(updatedUser)
                            
                            // Update session for greeting consistency
                            sessionManager.saveSession(
                                isLoggedIn = true,
                                name = finalName,
                                isFemale = isFemaleValue,
                                role = current.role,
                                email = current.email
                            )
                            
                            // Reset edit modes
                            editNombre = false
                            editCedula = false
                            editFechaNac = false
                            editSexo = false
                            editTelefono = false
                            editDireccion = false
                            
                            scope.launch {
                                snackbarHostState.showSnackbar("Cambios guardados correctamente")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, colorPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Guardar cambios", color = colorPrimary, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Cerrar sesión", color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Host para el aviso de guardado (Snackbar) al frente de todo
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
fun EditableProfileField(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
    accentColor: Color,
    canEdit: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.5.sp)
            if (canEdit) {
                IconButton(onClick = onToggleEdit, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar",
                        tint = if (isEditing) accentColor else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        if (isEditing) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = accentColor,
                    unfocusedIndicatorColor = Color(0xFFE2E8F0)
                )
            )
        } else {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
fun ProfileDivider() {
    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF1F5F9))
}

