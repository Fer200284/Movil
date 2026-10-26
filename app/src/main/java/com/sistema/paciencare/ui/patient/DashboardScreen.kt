package com.sistema.paciencare.ui.patient

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.sistema.paciencare.R
import androidx.compose.ui.platform.LocalContext
import com.sistema.paciencare.data.local.SessionManager
import com.sistema.paciencare.model.Appointment
import com.sistema.paciencare.model.Exam
import com.sistema.paciencare.model.Medication
import com.sistema.paciencare.viewmodel.PatientViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    patientViewModel: PatientViewModel,
    onLogout: () -> Unit,
    onNavigateToEvaluation: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToMedications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToExams: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Usuario observado desde el ViewModel
    val user by patientViewModel.currentUser.collectAsState()
    
    val userName = user?.name ?: sessionManager.getUserName() ?: "Usuario"
    val initials = if (userName.length >= 2) userName.take(2).uppercase() else "U"
    
    // Prioritize user object from DB, fallback to SessionManager
    val isUserFemale = user?.isFemale ?: sessionManager.isFemale()

    val scrollState = rememberScrollState()
    val colorPrimary = Color(0xFF0284C7)
    val colorSecondary = Color(0xFF0EA5E9)
    val colorBg = Color(0xFFF1F5F9)

    // Estados observados desde el ViewModel
    val activeMedsEntities by patientViewModel.activeMedications.collectAsState()
    val recentExamsEntities by patientViewModel.allExams.collectAsState()
    val allAppointmentsEntities by patientViewModel.allAppointments.collectAsState()

    // Mapeo simple de Entidades a Modelos de UI (Sin duplicados)
    val medications = activeMedsEntities
        .distinctBy { it.name.trim().lowercase() }
        .map { 
            Medication(it.id, it.name,
                it.dose, it.frequency,
            it.duration, it.doctor, it.status)
        }.take(5)

    val exams = recentExamsEntities
        .distinctBy { "${it.name.trim().lowercase()}_${it.date}" }
        .map { 
            Exam(it.id, it.name, it.doctor, it.date, it.time, it.indications, it.status,
                Color(it.statusColor))
        }.take(5)

    val allAppointments = allAppointmentsEntities
        .distinctBy { "${it.date}_${it.doctor}" }
        .map { 
            Appointment(it.id,
                it.date, it.time, it.doctor, it.reason, it.status)
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

    // Mostrar notificación efímera según el estado de conexión real
    val connectionStatus by patientViewModel.connectionStatus.collectAsState()
    LaunchedEffect(connectionStatus) {
        if (connectionStatus.isNotEmpty()) {
            android.widget.Toast.makeText(context, connectionStatus, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Refrescar datos en tiempo real al ingresar al Dashboard
    LaunchedEffect(Unit) {
        patientViewModel.refreshData()
    }

    // Logic para seleccionar la verdadera próxima cita futura de forma reactiva
    val nextAppointment = remember(allAppointmentsEntities) {
        val sdfIso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val sdfSlash = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time

        fun parseDate(d: String): java.util.Date? {
            return try {
                if (d.contains("-")) sdfIso.parse(d.take(10)) else sdfSlash.parse(d.take(10))
            } catch (_: Exception) { null }
        }

        val futureOrPending = allAppointments.filter { appt ->
            val date = parseDate(appt.date)
            val isFutureOrToday = date != null && !date.before(today)
            val isPendingOrConfirmed = appt.status.equals("Confirmada", ignoreCase = true) || appt.status.equals("Pendiente", ignoreCase = true)
            isFutureOrToday && isPendingOrConfirmed
        }.sortedBy { parseDate(it.date) }

        futureOrPending.firstOrNull()
            ?: allAppointments.filter { appt -> appt.status.equals("Confirmada", ignoreCase = true) || appt.status.equals("Pendiente", ignoreCase = true) }
                .sortedByDescending { parseDate(it.date) }
                .firstOrNull()
            ?: allAppointments.firstOrNull()
    }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
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
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. HEADER PREMIUM COMPACTO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(colorPrimary, colorSecondary)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier.size(42.dp).shadow(4.dp, CircleShape),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Image(painter = painterResource(id = R.drawable.paci), contentDescription = null, modifier = Modifier.fillMaxSize().padding(6.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isUserFemale) "Bienvenida de nuevo" else "Bienvenido de nuevo", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                userName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = { showLogoutDialog = true },
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                            Text(initials, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Cuerpo
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Próxima cita", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 8.dp))
                if (nextAppointment != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.DateRange, null, tint = colorPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(nextAppointment.date, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Schedule, null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(nextAppointment.time, fontSize = 13.sp, color = Color(0xFF64748B))
                                Spacer(modifier = Modifier.width(14.dp))
                                Icon(Icons.Filled.Person, null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(nextAppointment.doctor, fontSize = 13.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                } else {
                    Text("No tienes citas programadas", color = Color(0xFF64748B), fontSize = 13.sp, modifier = Modifier.padding(vertical = 10.dp))
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3 Accesos Rápidos
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionItem("Mis citas", Icons.Filled.Event, Modifier.weight(1f)) { onNavigateToAppointments() }
                    QuickActionItem("Medicamentos", Icons.Filled.Medication, Modifier.weight(1f)) { onNavigateToMedications() }
                    QuickActionItem("Exámenes", Icons.Filled.Science, Modifier.weight(1f)) { onNavigateToExams() } 
                }

                Spacer(modifier = Modifier.height(20.dp))

                SectionHeader("Últimos medicamentos", onSeeAll = onNavigateToMedications)
                medications.forEach { med ->
                    MedicationItem(med)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                SectionHeader("Últimos exámenes", onSeeAll = onNavigateToExams)
                exams.forEach { exam ->
                    ExamItem(exam)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            // BARRA DE ACCIÓN INFERIOR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .navigationBarsPadding()
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onNavigateToEvaluation,
                        modifier = Modifier.weight(1.2f).height(54.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                    ) {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Evaluar atención", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.weight(1f).height(54.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                    ) {
                        Icon(Icons.Filled.Person, null, tint = colorPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mi perfil", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(90.dp).clickable { onClick() }.shadow(2.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color(0xFF0284C7), modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF1E293B))
        }
    }
}

@Composable
fun MedicationItem(med: Medication) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Medication, null, tint = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(med.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(med.dose, fontSize = 12.sp, color = Color(0xFF64748B))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFF0FDF4)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(med.duration, fontSize = 12.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (expanded) "Ocultar" else "Ver",
                        fontSize = 13.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))
                    if (med.frequency.isNotBlank()) {
                        Text("Indicación médica / Frecuencia:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Text(med.frequency, fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text("Médico responsable:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Text(med.doctor, fontSize = 13.sp, color = Color(0xFF1E293B))
                }
            }
        }
    }
}

@Composable
fun ExamItem(exam: Exam) {
    var expanded by remember { mutableStateOf(false) }

    val displayName = if (exam.name.isNotBlank() && !exam.name.equals("Examen de Laboratorio", ignoreCase = true)) exam.name else "Hemoglobina Glicosilada (HbA1c)"
    val displayIndications = if (exam.indications.isNotBlank()) exam.indications else "Ayuno de 8 horas"

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Science, null, tint = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(
                        text = if (exam.time.isNotBlank()) "${exam.date} · ${exam.time}" else exam.date,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(exam.statusColor.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(exam.status, fontSize = 12.sp, color = exam.statusColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (expanded) "Ocultar" else "Ver",
                        fontSize = 13.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = displayIndications,
                            fontSize = 12.sp,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Médico responsable:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Text(exam.doctor, fontSize = 13.sp, color = Color(0xFF1E293B))
                }
            }
        }
    }
}
@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Text("Ver todos", fontSize = 14.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSeeAll() })
    }
}
