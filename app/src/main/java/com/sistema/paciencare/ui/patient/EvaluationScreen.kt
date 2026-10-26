package com.sistema.paciencare.ui.patient

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.sistema.paciencare.data.local.entities.EvaluationEntity
import com.sistema.paciencare.viewmodel.PatientViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(
    patientViewModel: PatientViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val colorPrimary = Color(0xFF0284C7)
    val colorAccent = Color(0xFF10B981)
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val user by (patientViewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) })
    val myEvaluations by (patientViewModel?.allEvaluations?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    var activeTab by remember { mutableIntStateOf(0) } // 0: Nueva Evaluación, 1: Mis Evaluaciones Enviadas

    // Estado del DatePicker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedConsultationDateMillis by remember { mutableStateOf<Long?>(null) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedConsultationDateMillis = millis
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

    var r1 by remember { mutableIntStateOf(5) }
    var r2 by remember { mutableIntStateOf(5) }
    var r3 by remember { mutableIntStateOf(5) }
    var r4 by remember { mutableIntStateOf(5) }
    var r5 by remember { mutableIntStateOf(5) }
    var comments by remember { mutableStateOf("") }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                patientViewModel?.refreshData()
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
        modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
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
                        Text("Evaluar atención", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Tu opinión nos importa", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Card(
                        modifier = Modifier.size(50.dp).shadow(4.dp, CircleShape),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                            Image(painter = painterResource(id = R.drawable.paci), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            // Barra de Pestañas (Nueva Evaluación vs Mis Evaluaciones Enviadas)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tabs = listOf("Nueva Evaluación", "Ver Mis Evaluaciones")
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFE2F2FE) else Color.Transparent)
                                .clickable { activeTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tabTitle,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colorPrimary else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 10.dp)) {
                if (activeTab == 0) {
                    // PESTAÑA 0: NUEVA EVALUACIÓN
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F2FE).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val displayDate = remember(selectedConsultationDateMillis, isRefreshing) {
                                    if (selectedConsultationDateMillis != null) {
                                        SimpleDateFormat("d 'de' MMMM yyyy", Locale("es", "ES")).format(Date(selectedConsultationDateMillis!!))
                                    } else {
                                        SimpleDateFormat("d 'de' MMMM yyyy", Locale("es", "ES")).format(Date())
                                    }
                                }
                                Text("Consulta del $displayDate", fontSize = 14.sp, color = colorPrimary, fontWeight = FontWeight.Bold)
                                Text("Centro de Salud Tisma · Consulta General", fontSize = 13.sp, color = Color(0xFF64748B))
                            }
                            
                            // Botón de Calendario
                            Surface(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = "Elegir Fecha",
                                        tint = colorPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cambiar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    RatingQuestion("¿Qué tan satisfecho quedó con la atención recibida?", r1) { r1 = it }
                    RatingQuestion("¿Cuánto tiempo tuvo que esperar?", r2) { r2 = it }
                    RatingQuestion("¿Cómo le pareció la atención del personal?", r3) { r3 = it }
                    RatingQuestion("¿Cómo le pareció la consulta con el doctor?", r4) { r4 = it }
                    RatingQuestion("¿El doctor resolvió sus dudas?", r5) { r5 = it }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Comentarios adicionales", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = comments,
                                onValueChange = { comments = it },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                placeholder = { Text("Comparte cualquier comentario o sugerencia...", fontSize = 14.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colorPrimary, unfocusedBorderColor = Color(0xFFF1F5F9), focusedContainerColor = Color(0xFFF8FAFC), unfocusedContainerColor = Color(0xFFF8FAFC))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val avg = (r1 + r2 + r3 + r4 + r5) / 5.0f
                            val dateNow = if (selectedConsultationDateMillis != null) {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedConsultationDateMillis!!))
                            } else {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            }
                            val newEvaluation = EvaluationEntity(
                                userEmail = user?.email ?: "paciente@paciencare.com",
                                rating1 = r1,
                                rating2 = r2,
                                rating3 = r3,
                                rating4 = r4,
                                rating5 = r5,
                                overallRating = avg,
                                comments = comments.trim(),
                                date = dateNow
                            )
                            patientViewModel?.insertEvaluation(newEvaluation)
                            Toast.makeText(context, "¡Gracias por tu evaluación!", Toast.LENGTH_SHORT).show()
                            activeTab = 1 // Cambiar a la pestaña de historial enviado
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp).shadow(6.dp, RoundedCornerShape(14.dp)).background(colorAccent, RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), shape = RoundedCornerShape(14.dp)
                    ) { Text("Enviar evaluación", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))) {
                        Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }
                } else {
                    // PESTAÑA 1: VER MIS EVALUACIONES ENVIADAS
                    var showDeleteEvalDialog by remember { mutableStateOf(false) }
                    var evalToDelete by remember { mutableStateOf<EvaluationEntity?>(null) }

                    if (showDeleteEvalDialog && evalToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { showDeleteEvalDialog = false },
                            title = { Text("¿Eliminar evaluación?", fontWeight = FontWeight.Bold) },
                            text = { Text("Se quitará esta evaluación de tu historial local. ¿Deseas continuar?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    evalToDelete?.let { patientViewModel?.deleteEvaluation(it) }
                                    showDeleteEvalDialog = false
                                }) {
                                    Text("Eliminar", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteEvalDialog = false }) {
                                    Text("Cancelar", color = Color(0xFF64748B))
                                }
                            },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Historial de Evaluaciones Enviadas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

                        if (myEvaluations.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No has enviado evaluaciones de atención aún.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        } else {
                            myEvaluations.forEach { eval ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "${String.format(Locale.US, "%.1f", eval.overallRating)} / 5.0",
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF1E293B)
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFFE2F2FE))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        eval.date,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colorPrimary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = {
                                                        evalToDelete = eval
                                                        showDeleteEvalDialog = true
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = "Eliminar evaluación",
                                                        tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Encabezado de la Tabla
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Desglose de Calificaciones", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

                                            // Logotipo Paci en círculo blanco
                                            Card(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .shadow(2.dp, CircleShape),
                                                shape = CircleShape,
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.paci),
                                                        contentDescription = "Logo Paci",
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Tabla Estilo Excel
                                        EvaluationExcelTable(eval)

                                        if (eval.comments.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text("Comentario:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text("\"${eval.comments}\"", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun EvaluationExcelTable(eval: EvaluationEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cabecera de la Tabla Excel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ASPECTO EVALUADO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF334155)
                )
                Text(
                    "PUNTAJE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF334155)
                )
            }
            HorizontalDivider(color = Color(0xFFCBD5E1))

            // Filas Cebra Estilo Tabla Excel
            ExcelTableRow("Atención médica recibida", eval.rating1, isEven = true)
            HorizontalDivider(color = Color(0xFFE2E8F0))
            ExcelTableRow("Tiempo de espera", eval.rating2, isEven = false)
            HorizontalDivider(color = Color(0xFFE2E8F0))
            ExcelTableRow("Atención del personal", eval.rating3, isEven = true)
            HorizontalDivider(color = Color(0xFFE2E8F0))
            ExcelTableRow("Consulta con el doctor", eval.rating4, isEven = false)
            HorizontalDivider(color = Color(0xFFE2E8F0))
            ExcelTableRow("Resolución de dudas", eval.rating5, isEven = true)
        }
    }
}

@Composable
fun ExcelTableRow(aspect: String, score: Int, isEven: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isEven) Color.White else Color(0xFFF8FAFC))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = aspect,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (score >= 4) Color(0xFFF0FDF4) else Color(0xFFFEF3C7))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "★ $score / 5",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (score >= 4) Color(0xFF10B981) else Color(0xFFF59E0B)
            )
        }
    }
}


@Composable
fun RatingQuestion(
    question: String,
    currentRating: Int,
    onRatingSelected: (Int) -> Unit
) {
    val (label, starColor) = when (currentRating) {
        1 -> "Muy malo" to Color.Red
        2 -> "Malo" to Color.Red
        3 -> "Bueno" to Color(0xFF10B981)
        4 -> "Muy bueno" to Color(0xFF10B981)
        5 -> "¡Excelente!" to Color(0xFFF59E0B)
        else -> "" to Color(0xFFE2E8F0)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(question, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(5) { index ->
                    val starIndex = index + 1
                    Icon(
                        imageVector = if (starIndex <= currentRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = if (starIndex <= currentRating) starColor else Color(0xFFE2E8F0),
                        modifier = Modifier.size(36.dp).clickable { onRatingSelected(starIndex) }
                    )
                }
            }
            if (label.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = starColor)
            }
        }
    }
}
