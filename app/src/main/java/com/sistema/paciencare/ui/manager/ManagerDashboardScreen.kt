package com.sistema.paciencare.ui.manager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.sistema.paciencare.R
import com.sistema.paciencare.data.local.entities.AppointmentEntity
import com.sistema.paciencare.data.local.entities.MedicationEntity
import com.sistema.paciencare.viewmodel.PatientViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    patientViewModel: PatientViewModel? = null,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val colorPrimary = Color(0xFF0284C7)
    val colorSecondary = Color(0xFF0EA5E9)
    val colorBg = Color(0xFFF1F5F9)
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Resumen, 1: Citas, 2: Medicamentos, 3: Satisfacción

    // Estado del Filtro de Calendario
    var selectedSpecificDate by remember { mutableStateOf<String?>(null) } // e.g. "15/09/2026"

    // Lógica Calendario DatePicker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        selectedSpecificDate = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Observación de datos globales de todos los pacientes desde ViewModel / BD Local Room
    val appointmentsEntities by (patientViewModel?.globalAllAppointments?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val medicationsEntities by (patientViewModel?.globalAllMedications?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val evaluationsEntities by (patientViewModel?.globalAllEvaluations?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val userEntity by (patientViewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) })

    // Filtrado dinámico de citas por Calendario
    val filteredAppointments = remember(appointmentsEntities, selectedSpecificDate) {
        if (!selectedSpecificDate.isNullOrEmpty()) {
            appointmentsEntities.filter { it.date == selectedSpecificDate }
        } else {
            appointmentsEntities
        }
    }

    // Filtrado dinámico de encuestas por Calendario
    val filteredEvaluations = remember(evaluationsEntities, selectedSpecificDate) {
        if (!selectedSpecificDate.isNullOrEmpty()) {
            evaluationsEntities.filter { it.date == selectedSpecificDate }
        } else {
            evaluationsEntities
        }
    }

    // Cálculo dinámico de conteos de 12 meses
    val monthlyCounts12 = remember(filteredAppointments) {
        (1..12).map { monthNum ->
            val monthCode = if (monthNum < 10) "0$monthNum" else "$monthNum"
            filteredAppointments.count { appt ->
                appt.date.contains("/$monthCode/") || appt.date.contains("-$monthCode-")
            }
        }
    }

    val managerTreatmentsCount by (patientViewModel?.managerTreatmentsCount?.collectAsState() ?: remember { mutableIntStateOf(0) })

    // Métricas
    val totalAppointmentsCount = filteredAppointments.size
    val completedAppointmentsCount = filteredAppointments.count { it.status.equals("Realizada", ignoreCase = true) }
    val pendingAppointmentsCount = filteredAppointments.count { it.status.equals("Pendiente", ignoreCase = true) || it.status.equals("Confirmada", ignoreCase = true) }
    val cancelledAppointmentsCount = filteredAppointments.count { it.status.equals("Cancelada", ignoreCase = true) }
    val totalMedsTypes = medicationsEntities.size
    val activeTreatmentsCount = managerTreatmentsCount

    // Métricas reales de Evaluaciones / Encuestas filtradas por fecha
    val evaluationsCount = filteredEvaluations.size
    val averageScore = if (evaluationsCount > 0) String.format(Locale.US, "%.1f", filteredEvaluations.map { it.overallRating }.average().toFloat()) else "0.0"

    val count5 = filteredEvaluations.count { it.overallRating >= 4.5f }
    val count4 = filteredEvaluations.count { it.overallRating in 3.5f..<4.5f }
    val count3 = filteredEvaluations.count { it.overallRating in 2.5f..<3.5f }
    val count2 = filteredEvaluations.count { it.overallRating in 1.5f..<2.5f }
    val count1 = filteredEvaluations.count { it.overallRating < 1.5f }

    val pct5 = if (evaluationsCount > 0) count5.toFloat() / evaluationsCount else 0f
    val pct4 = if (evaluationsCount > 0) count4.toFloat() / evaluationsCount else 0f
    val pct3 = if (evaluationsCount > 0) count3.toFloat() / evaluationsCount else 0f
    val pct2 = if (evaluationsCount > 0) count2.toFloat() / evaluationsCount else 0f
    val pct1 = if (evaluationsCount > 0) count1.toFloat() / evaluationsCount else 0f

    val avg1 = if (evaluationsCount > 0) filteredEvaluations.map { it.rating1 }.average().toFloat() else 0f
    val avg2 = if (evaluationsCount > 0) filteredEvaluations.map { it.rating2 }.average().toFloat() else 0f
    val avg3 = if (evaluationsCount > 0) filteredEvaluations.map { it.rating3 }.average().toFloat() else 0f
    val avg4 = if (evaluationsCount > 0) filteredEvaluations.map { it.rating4 }.average().toFloat() else 0f
    val avg5 = if (evaluationsCount > 0) filteredEvaluations.map { it.rating5 }.average().toFloat() else 0f

    val periodText = if (!selectedSpecificDate.isNullOrEmpty()) "Fecha: $selectedSpecificDate" else "Todos los registros"

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("¿Cerrar sesión?", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas salir de tu cuenta?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onLogout()
                }) {
                    Text("Cerrar sesión", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Cargar datos globales consolidados de la clínica al ingresar
    LaunchedEffect(Unit) {
        patientViewModel?.syncManagerData()
    }

    // Mostrar notificación efímera al ingresar al panel de gerencia según la conexión real
    val connectionStatus by (patientViewModel?.connectionStatus?.collectAsState() ?: remember { mutableStateOf("Conectado localmente") })
    LaunchedEffect(connectionStatus) {
        if (connectionStatus.isNotEmpty()) {
            android.widget.Toast.makeText(context, connectionStatus, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                patientViewModel?.syncManagerData()
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
            // 1. HEADER GERENCIAL CON GRADIENTE Y BOTÓN PDF
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(colorPrimary, colorSecondary)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Card(
                                modifier = Modifier
                                    .size(46.dp)
                                    .shadow(6.dp, CircleShape),
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.paci),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Panel Administrativo",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val currentMonthYear = remember {
                                    val sdf = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                    val formatted = sdf.format(Date())
                                    formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                }
                                Text(
                                    "Centro de Salud Tisma · $currentMonthYear",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = { showConfirmDialog = true },
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Salir",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Salir",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = userEntity?.name?.take(2)?.uppercase() ?: "GR"
                                Text(
                                    initials,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón de Imprimir / Exportar PDF según la pestaña activa
                    Button(
                        onClick = {
                            val tabName = when (selectedTab) {
                                0 -> "Resumen General"
                                1 -> "Citas Médicas"
                                2 -> "Medicamentos"
                                else -> "Satisfacción del Cliente"
                            }
                            val metrics = when (selectedTab) {
                                0 -> listOf(
                                    "Pacientes Atendidos" to "$completedAppointmentsCount",
                                    "Citas Realizadas" to "$completedAppointmentsCount",
                                    "Citas Pendientes" to "$pendingAppointmentsCount",
                                    "Citas Canceladas" to "$cancelledAppointmentsCount"
                                )
                                1 -> listOf(
                                    "Total de Citas" to "$totalAppointmentsCount",
                                    "Citas Pendientes" to "$pendingAppointmentsCount",
                                    "Citas Canceladas" to "$cancelledAppointmentsCount",
                                    "Citas Realizadas" to "$completedAppointmentsCount"
                                )
                                2 -> listOf(
                                    "Tipos de Medicamentos" to "$totalMedsTypes",
                                    "Tratamientos Activos" to "$activeTreatmentsCount"
                                )
                                else -> listOf(
                                    "Promedio General" to "$averageScore / 5.0",
                                    "Total Encuestas" to "$evaluationsCount",
                                    "Atención Médica Recibida" to "${String.format(Locale.US, "%.1f", avg1)} / 5.0",
                                    "Tiempo de Espera" to "${String.format(Locale.US, "%.1f", avg2)} / 5.0",
                                    "Trato del Personal" to "${String.format(Locale.US, "%.1f", avg3)} / 5.0",
                                    "Consulta del Doctor" to "${String.format(Locale.US, "%.1f", avg4)} / 5.0",
                                    "Resolución de Dudas" to "${String.format(Locale.US, "%.1f", avg5)} / 5.0"
                                )
                            }
                            val chartData = listOf(0, 0, 0, 0, 0, 0, 0, 0, totalAppointmentsCount, 0, 0, 0)
                            PdfExporter.printOrSavePdf(context, tabName, selectedTab, periodText, metrics, chartData)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = "PDF",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Exportar PDF / Imprimir Informe",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 2. BARRA DE PERIODO (ÚNICAMENTE BOTÓN CALENDARIO Y LOGO PACI)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(1.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // LOGO PACI
                        Card(
                            modifier = Modifier
                                .size(28.dp)
                                .shadow(2.dp, CircleShape),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.paci),
                                contentDescription = "Logo Paci",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Periodo:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // BOTÓN CALENDARIO
                        Surface(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE2F2FE)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = "Calendario",
                                    tint = colorPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    selectedSpecificDate ?: "Fecha",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorPrimary
                                )
                            }
                        }

                        if (!selectedSpecificDate.isNullOrEmpty()) {
                            // Botón Limpiar Fecha
                            IconButton(
                                onClick = { selectedSpecificDate = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Limpiar día",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. BARRA DE PESTAÑAS (Resumen, Citas, Medicamentos, Satisfacción)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tabs = listOf("Resumen", "Citas", "Medicamentos", "Satisfacción")
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Color(0xFFE2F2FE) else Color.Transparent
                                )
                                .clickable { selectedTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = tabTitle,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colorPrimary else Color(0xFF64748B)
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(colorPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. CONTENIDO DINÁMICO
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                when (selectedTab) {
                    0 -> TabResumenContent(
                        totalPatients = totalAppointmentsCount,
                        completedAppointments = completedAppointmentsCount,
                        pendingAppointments = pendingAppointmentsCount,
                        cancelledAppointments = cancelledAppointmentsCount,
                        monthlyCounts12 = monthlyCounts12,
                        hasEvaluations = evaluationsCount > 0,
                        colorPrimary = colorPrimary
                    )
                    1 -> TabCitasContent(
                        appointmentsEntities = filteredAppointments,
                        totalAppointments = totalAppointmentsCount,
                        pendingAppointments = pendingAppointmentsCount,
                        cancelledAppointments = cancelledAppointmentsCount,
                        monthlyCounts12 = monthlyCounts12,
                        colorPrimary = colorPrimary
                    )
                    2 -> TabMedicamentosContent(
                        medicationsEntities = medicationsEntities,
                        medsTypes = totalMedsTypes,
                        treatments = activeTreatmentsCount,
                        colorPrimary = colorPrimary
                    )
                    3 -> TabSatisfaccionContent(
                        evaluationsEntities = filteredEvaluations,
                        evaluationsCount = evaluationsCount,
                        averageScore = averageScore,
                        pct5 = pct5,
                        pct4 = pct4,
                        pct3 = pct3,
                        pct2 = pct2,
                        pct1 = pct1,
                        avg1 = avg1,
                        avg2 = avg2,
                        avg3 = avg3,
                        avg4 = avg4,
                        avg5 = avg5,
                        colorPrimary = colorPrimary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==========================================================
// PESTAÑA 0: RESUMEN
// ==========================================================
@Composable
fun TabResumenContent(
    totalPatients: Int,
    completedAppointments: Int,
    pendingAppointments: Int,
    cancelledAppointments: Int,
    monthlyCounts12: List<Int>,
    hasEvaluations: Boolean,
    colorPrimary: Color
) {
    val months12 = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Grid de 4 Métricas de altura pareja (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "PACIENTES ATENDIDOS",
                value = totalPatients.toString(),
                subtext = if (totalPatients > 0) "+$totalPatients en periodo" else "Sin registros",
                icon = Icons.Filled.People,
                iconBg = Color(0xFFE2F2FE),
                iconColor = colorPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "CITAS REALIZADAS",
                value = completedAppointments.toString(),
                subtext = if (completedAppointments > 0) "+$completedAppointments en periodo" else "Sin registros",
                icon = Icons.Filled.CheckBox,
                iconBg = Color(0xFFF0FDF4),
                iconColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "CITAS PENDIENTES",
                value = pendingAppointments.toString(),
                subtext = if (pendingAppointments > 0) "+$pendingAppointments en periodo" else "Sin registros",
                icon = Icons.Filled.HourglassTop,
                iconBg = Color(0xFFFEF3C7),
                iconColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "CITAS CANCELADAS",
                value = cancelledAppointments.toString(),
                subtext = if (cancelledAppointments > 0) "+$cancelledAppointments en periodo" else "Sin registros",
                icon = Icons.Filled.Cancel,
                iconBg = Color(0xFFFEE2E2),
                iconColor = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }

        // Gráfico 1: Citas por mes (12 Meses)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Citas por mes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(16.dp))
                BarChartComposable(
                    data = monthlyCounts12,
                    labels = months12,
                    barColor = colorPrimary
                )
            }
        }

        // Gráfico 2: Satisfacción general
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Satisfacción general",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(16.dp))
                DonutChartComposable(hasData = hasEvaluations)
            }
        }
    }
}

// ==========================================================
// PESTAÑA 1: CITAS
// ==========================================================
@Composable
fun TabCitasContent(
    appointmentsEntities: List<AppointmentEntity>,
    totalAppointments: Int,
    pendingAppointments: Int,
    cancelledAppointments: Int,
    monthlyCounts12: List<Int>,
    colorPrimary: Color
) {
    val months12 = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Métricas de Citas de altura uniforme (1x3)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallStatCard(
                title = "TOTAL CITAS",
                value = totalAppointments.toString(),
                icon = Icons.Filled.DateRange,
                iconBg = Color(0xFFE2F2FE),
                iconColor = colorPrimary,
                modifier = Modifier.weight(1f)
            )
            SmallStatCard(
                title = "PENDIENTES",
                value = pendingAppointments.toString(),
                icon = Icons.Filled.HourglassTop,
                iconBg = Color(0xFFFEF3C7),
                iconColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            SmallStatCard(
                title = "CANCELADAS",
                value = cancelledAppointments.toString(),
                icon = Icons.Filled.Cancel,
                iconBg = Color(0xFFFEE2E2),
                iconColor = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }

        // Gráfico de línea: Tendencia 12 meses
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Citas por mes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LineChartComposable(
                    data = monthlyCounts12,
                    labels = months12,
                    lineColor = colorPrimary
                )
            }
        }

        // Gráfico de barras: Horarios más solicitados
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Horarios más solicitados",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(16.dp))
                val hourCounts = remember(appointmentsEntities) {
                    listOf("8", "9", "10", "11", "2", "3", "4").map { h ->
                        appointmentsEntities.count { it.time.contains(h) }
                    }
                }
                BarChartComposable(
                    data = hourCounts,
                    labels = listOf("8am", "9am", "10am", "11am", "2pm", "3pm", "4pm"),
                    barColor = Color(0xFF10B981)
                )
            }
        }

        // Citas por doctor
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Citas por doctor",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                val doctorsGrouped = remember(appointmentsEntities) {
                    appointmentsEntities
                        .filter { it.doctor.isNotBlank() }
                        .groupBy { it.doctor }
                        .mapValues { it.value.size }
                }
                
                if (doctorsGrouped.isEmpty()) {
                    Text(
                        "Sin citas registradas por doctor",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    val maxCitas = totalAppointments.coerceAtLeast(1)
                    doctorsGrouped.forEach { (docName, count) ->
                        DoctorRankItem(docName, count, maxCitas, colorPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

// ==========================================================
// PESTAÑA 2: MEDICAMENTOS
// ==========================================================
@Composable
fun TabMedicamentosContent(
    medicationsEntities: List<MedicationEntity>,
    medsTypes: Int,
    treatments: Int,
    colorPrimary: Color
) {
    val medsGrouped = remember(medicationsEntities) {
        medicationsEntities
            .filter { it.name.isNotBlank() }
            .groupBy { it.name }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "MEDICAMENTOS",
                value = medsTypes.toString(),
                subtext = if (medsTypes > 0) "tipos registrados" else "Sin registros",
                icon = Icons.Filled.Medication,
                iconBg = Color(0xFFF0FDF4),
                iconColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "TRATAMIENTOS",
                value = treatments.toString(),
                subtext = if (treatments > 0) "tratamientos activos" else "Sin registros",
                icon = Icons.AutoMirrored.Filled.Assignment,
                iconBg = Color(0xFFE2F2FE),
                iconColor = colorPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Gráfico horizontal: Medicamentos más utilizados
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Medicamentos más utilizados",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (medsGrouped.isEmpty()) {
                    Text("No hay medicamentos registrados en la base de datos", fontSize = 13.sp, color = Color(0xFF94A3B8))
                } else {
                    HorizontalBarChartComposable(
                        data = medsGrouped,
                        barColor = Color(0xFF10B981)
                    )
                }
            }
        }

        // Lista de Pacientes por medicamento
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Pacientes por medicamento",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (medsGrouped.isEmpty()) {
                    Text("No hay medicamentos registrados en la base de datos", fontSize = 13.sp, color = Color(0xFF94A3B8))
                } else {
                    medsGrouped.forEachIndexed { index, (name, count) ->
                        MedicationPatientRow(name, count, colorPrimary)
                        if (index < medsGrouped.size - 1) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// PESTAÑA 3: SATISFACCIÓN (100% REAL DE LA BD)
// ==========================================================
@Composable
fun TabSatisfaccionContent(
    evaluationsEntities: List<com.sistema.paciencare.data.local.entities.EvaluationEntity> = emptyList(),
    evaluationsCount: Int,
    averageScore: String,
    pct5: Float,
    pct4: Float,
    pct3: Float,
    pct2: Float,
    pct1: Float,
    avg1: Float,
    avg2: Float,
    avg3: Float,
    avg4: Float,
    avg5: Float,
    colorPrimary: Color
) {
    val hasData = evaluationsCount > 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Puntuación General
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Población Encuestada",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$averageScore / 5.0",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        if (hasData) "Basado en $evaluationsCount encuestas reales" else "Sin evaluaciones registradas aún",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (hasData) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (hasData) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Gráfico de Distribución de Puntuaciones por Estrellas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Distribución de Calificaciones",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (!hasData) {
                    Text("Sin datos de encuestas recibidas aún", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
                StarDistributionBar("5 ★ (Excelente)", pct5, Color(0xFF10B981))
                Spacer(modifier = Modifier.height(8.dp))
                StarDistributionBar("4 ★ (Muy bueno)", pct4, colorPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                StarDistributionBar("3 ★ (Bueno)", pct3, Color(0xFFF59E0B))
                Spacer(modifier = Modifier.height(8.dp))
                StarDistributionBar("2 ★ (Malo)", pct2, Color(0xFFF97316))
                Spacer(modifier = Modifier.height(8.dp))
                StarDistributionBar("1 ★ (Muy malo)", pct1, Color(0xFFEF4444))
            }
        }

        // Desglose por Aspectos
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Calificación por aspecto",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(14.dp))
                SatisfactionAspectRow("Atención médica recibida", avg1, colorPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                SatisfactionAspectRow("Tiempo de espera", avg2, Color(0xFF10B981))
                Spacer(modifier = Modifier.height(10.dp))
                SatisfactionAspectRow("Trato del personal", avg3, Color(0xFFF59E0B))
                Spacer(modifier = Modifier.height(10.dp))
                SatisfactionAspectRow("Consulta del doctor", avg4, Color(0xFF0284C7))
                Spacer(modifier = Modifier.height(10.dp))
                SatisfactionAspectRow("Resolución de dudas", avg5, Color(0xFF8B5CF6))
            }
        }

        // Opiniones y Comentarios Recientes de Pacientes
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Opiniones y Comentarios Recientes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val commentsList = evaluationsEntities.filter { it.comments.isNotBlank() }
                if (commentsList.isEmpty()) {
                    Text("No hay comentarios adicionales en las encuestas aún", fontSize = 13.sp, color = Color(0xFF94A3B8))
                } else {
                    commentsList.take(5).forEachIndexed { index, eval ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    eval.userEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                                Text(eval.date, fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "\"${eval.comments}\"",
                                fontSize = 13.sp,
                                color = Color(0xFF334155),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (index < commentsList.take(5).size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// COMPONENTES AUXILIARES Y GRÁFICOS PERSONALIZADOS
// ==========================================================

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String?,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .heightIn(min = 110.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.2.sp,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Column {
                Text(
                    value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                if (subtext != null) {
                    Text(
                        subtext,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

@Composable
fun SmallStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .heightIn(min = 96.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(15.dp))
            }
            Column {
                Text(
                    title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Text(
                    value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }
    }
}

@Composable
fun BarChartComposable(
    data: List<Int>,
    labels: List<String>,
    barColor: Color
) {
    val maxValue = (data.maxOrNull() ?: 10).coerceAtLeast(1)
    val isAllZero = data.all { it == 0 }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isAllZero) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin citas registradas en el periodo", fontSize = 13.sp, color = Color(0xFF94A3B8))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { value ->
                    val fraction = if (maxValue > 0 && value > 0) value.toFloat() / maxValue else 0f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(if (labels.size > 8) 14.dp else 26.dp)
                                .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(if (value > 0) barColor else Color(0xFFE2E8F0))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                labels.forEach { label ->
                    Text(
                        label,
                        fontSize = if (labels.size > 8) 8.sp else 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun DonutChartComposable(hasData: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val strokeWidth = 32f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            if (!hasData) {
                drawArc(
                    color = Color(0xFFE2E8F0),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                // 52% Muy satisfecho (#0284C7)
                drawArc(
                    color = Color(0xFF0284C7),
                    startAngle = -90f,
                    sweepAngle = 360f * 0.52f - 4f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                // 28% Satisfecho (#10B981)
                drawArc(
                    color = Color(0xFF10B981),
                    startAngle = -90f + (360f * 0.52f),
                    sweepAngle = 360f * 0.28f - 4f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                // 12% Neutral (#F59E0B)
                drawArc(
                    color = Color(0xFFF59E0B),
                    startAngle = -90f + (360f * 0.80f),
                    sweepAngle = 360f * 0.12f - 4f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                // 8% Insatisfecho (#EF4444)
                drawArc(
                    color = Color(0xFFEF4444),
                    startAngle = -90f + (360f * 0.92f),
                    sweepAngle = 360f * 0.08f - 4f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 12.dp)
        ) {
            DonutLegendItem("Muy satisfecho", if (hasData) "52%" else "0%", Color(0xFF0284C7))
            DonutLegendItem("Satisfecho", if (hasData) "28%" else "0%", Color(0xFF10B981))
            DonutLegendItem("Neutral", if (hasData) "12%" else "0%", Color(0xFFF59E0B))
            DonutLegendItem("Insatisfecho", if (hasData) "8%" else "0%", Color(0xFFEF4444))
        }
    }
}

@Composable
fun DonutLegendItem(label: String, percentage: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.width(150.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 11.sp, color = Color(0xFF1E293B))
        }
        Text(percentage, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

@Composable
fun LineChartComposable(
    data: List<Int>,
    labels: List<String>,
    lineColor: Color
) {
    val maxValue = (data.maxOrNull() ?: 10).coerceAtLeast(1)
    val isAllZero = data.all { it == 0 }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isAllZero) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin citas registradas en el periodo", fontSize = 13.sp, color = Color(0xFF94A3B8))
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val width = size.width
                val height = size.height
                val spacing = if (data.size > 1) width / (data.size - 1) else width

                val points = data.mapIndexed { index, value ->
                    val x = index * spacing
                    val y = height - (value.toFloat() / maxValue * (height - 20f)) - 10f
                    Offset(x, y)
                }

                val path = Path()
                if (points.isNotEmpty()) {
                    path.moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val control1 = Offset(p1.x + spacing / 2, p1.y)
                        val control2 = Offset(p2.x - spacing / 2, p2.y)
                        path.cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
                    }
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 5f)
                )

                points.forEach { point ->
                    drawCircle(color = Color.White, radius = 6f, center = point)
                    drawCircle(color = lineColor, radius = 4f, center = point)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        label,
                        fontSize = if (labels.size > 8) 8.sp else 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalBarChartComposable(
    data: List<Pair<String, Int>>,
    barColor: Color
) {
    val maxValue = (data.maxOfOrNull { it.second } ?: 10).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.forEach { (name, count) ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = barColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (maxValue > 0) count.toFloat() / maxValue else 0f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@Composable
fun DoctorRankItem(name: String, count: Int, max: Int, color: Color) {
    val safeMax = max.coerceAtLeast(1)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            Text("$count citas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { count.toFloat() / safeMax },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0xFFF1F5F9)
        )
    }
}

@Composable
fun MedicationPatientRow(name: String, count: Int, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE2F2FE))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("$count pacientes", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StarDistributionBar(label: String, fraction: Float, barColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.width(105.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF1F5F9))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "${(fraction * 100).toInt()}%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier.width(32.dp)
        )
    }
}

@Composable
fun SatisfactionAspectRow(aspect: String, score: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(aspect, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            Text(if (score > 0f) "${String.format(Locale.US, "%.1f", score)} / 5.0" else "0.0 / 5.0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (score > 0f) score / 5.0f else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0xFFF1F5F9)
        )
    }
}
