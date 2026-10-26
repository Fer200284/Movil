package com.sistema.paciencare.ui.patient

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.sistema.paciencare.R
import com.sistema.paciencare.model.Appointment
import com.sistema.paciencare.viewmodel.PatientViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentsScreen(patientViewModel: PatientViewModel, onBack: () -> Unit) {
    val colorPrimary = Color(0xFF0284C7)
    val colorBg = Color(0xFFF1F5F9)
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Refrescar datos en tiempo real al ingresar a Mis Citas
    LaunchedEffect(Unit) {
        patientViewModel.refreshData()
    }

    // Lista observada
    val appointmentsEntities by patientViewModel.allAppointments.collectAsState()

    // Mapeo (Sin duplicados)
    val appointmentsList = appointmentsEntities
        .distinctBy { "${it.date}_${it.doctor}" }
        .map { 
            Appointment(it.id, it.date, it.time, it.doctor, it.reason, it.status)
        }

    // Lógica de filtrado y ordenación dinámica (Próximas citas primero)
    val sdfIso = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    val sdfSlash = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }

    val filteredAppointments = remember(appointmentsList, searchQuery) {
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

        val matching = appointmentsList.filter { 
            it.doctor.contains(searchQuery, ignoreCase = true) || 
            it.date.contains(searchQuery, ignoreCase = true)
        }

        val (upcoming, past) = matching.partition { appt ->
            val date = parseDate(appt.date)
            date == null || !date.before(today)
        }

        upcoming.sortedBy { parseDate(it.date) } + past.sortedByDescending { parseDate(it.date) }
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
            // Header
            Box(
                modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(colorPrimary, Color(0xFF0EA5E9))))
                    .statusBarsPadding().padding(horizontal = 16.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White)
                            Text("Regresar", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Mis citas", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Historial y próximas citas", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    // Logotipo PacienCare arriba a la derecha (Estilo Bolita Blanca)
                    Card(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(4.dp, CircleShape),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
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

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                // Barra de Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    placeholder = { Text("Buscar por médico o fecha...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = colorPrimary) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Diálogos de Confirmación y Restricción
                var showPendingApptAlert by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                var appointmentToDelete by remember { mutableStateOf<Appointment?>(null) }

                if (showPendingApptAlert) {
                    AlertDialog(
                        onDismissRequest = { showPendingApptAlert = false },
                        title = { Text("Cita Programada Pendiente", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
                        text = { Text("Las citas programadas pendientes no se podrán eliminar a menos que ya haya transcurrido la fecha de la cita.", fontSize = 14.sp, color = Color(0xFF334155)) },
                        confirmButton = {
                            TextButton(onClick = { showPendingApptAlert = false }) {
                                Text("Entendido", color = colorPrimary, fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                if (showDeleteDialog && appointmentToDelete != null) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("¿Eliminar cita?", fontWeight = FontWeight.Bold) },
                        text = { Text("Esta acción no se puede deshacer. ¿Estás seguro de que quieres quitar esta cita de tu historial?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    appointmentToDelete?.let { appt ->
                                        appointmentsEntities.find { it.id == appt.id }?.let { entity ->
                                            patientViewModel.deleteAppointment(entity)
                                        }
                                    }
                                    showDeleteDialog = false
                                }
                            ) {
                                Text("Eliminar", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancelar", color = Color(0xFF64748B))
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                // Lista de Citas Filtrable
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (filteredAppointments.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron citas", color = Color(0xFF64748B), fontSize = 14.sp)
                        }
                    } else {
                        filteredAppointments.forEach { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                onDelete = { 
                                    val isPendingOrUpcoming = appointment.status.equals("Confirmada", ignoreCase = true) ||
                                            appointment.status.equals("Pendiente", ignoreCase = true)
                                    
                                    val isDateInFuture = try {
                                        val sdf = if (appointment.date.contains("-")) java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) else java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                        val apptDate = sdf.parse(appointment.date)
                                        val today = java.util.Date()
                                        apptDate != null && apptDate.after(today)
                                    } catch (_: Exception) { true }

                                    if (isPendingOrUpcoming && isDateInFuture) {
                                        showPendingApptAlert = true
                                    } else {
                                        appointmentToDelete = appointment
                                        showDeleteDialog = true
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(appointment: Appointment, onDelete: () -> Unit) {
    val today = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
    }

    val isPastDate = remember(appointment.date) {
        try {
            val sdfIso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val sdfSlash = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val date = if (appointment.date.contains("-")) sdfIso.parse(appointment.date.take(10)) else sdfSlash.parse(appointment.date.take(10))
            date != null && date.before(today)
        } catch (_: Exception) { false }
    }

    val displayStatus = if (isPastDate && appointment.status.equals("Confirmada", ignoreCase = true)) "Realizada" else appointment.status

    val statusColor = when (displayStatus) {
        "Confirmada" -> Color(0xFF0284C7)
        "Pendiente" -> Color(0xFFF59E0B)
        "Realizada" -> Color(0xFF10B981)
        "Cancelada" -> Color(0xFFEF4444)
        else -> Color(0xFF64748B)
    }
    
    val statusBg = statusColor.copy(alpha = 0.1f)

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Event, null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(appointment.date, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(" · ${appointment.time}", fontSize = 14.sp, color = Color(0xFF64748B))
                }
                
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(statusBg).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(displayStatus, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(appointment.doctor, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(appointment.reason, fontSize = 13.sp, color = Color(0xFF64748B))
                }
                
                // Botón de eliminar cita
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar cita",
                        tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}


