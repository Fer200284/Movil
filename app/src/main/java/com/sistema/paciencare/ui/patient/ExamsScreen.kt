package com.sistema.paciencare.ui.patient

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Science
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
import com.sistema.paciencare.model.Exam
import com.sistema.paciencare.viewmodel.PatientViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsScreen(patientViewModel: PatientViewModel, onBack: () -> Unit) {
    val colorPrimary = Color(0xFF0284C7)
    val colorBg = Color(0xFFF1F5F9)
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Sincronizar datos remotos al abrir la pantalla de exámenes
    LaunchedEffect(Unit) {
        patientViewModel.syncRemoteData()
    }

    // Lista observada
    val examsEntities by patientViewModel.allExams.collectAsState()

    // Mapeo (Sin duplicados)
    val examsList = examsEntities
        .distinctBy { "${it.name.trim().lowercase()}_${it.date}" }
        .map { 
            Exam(it.id, it.name, it.doctor, it.date, it.time, it.indications, it.status, Color(it.statusColor))
        }

    // Lógica de filtrado
    val filteredExams = examsList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.date.contains(searchQuery, ignoreCase = true)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var examToDelete by remember { mutableStateOf<Exam?>(null) }

    if (showDeleteDialog && examToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar resultado?", fontWeight = FontWeight.Bold) },
            text = { Text("Se borrará este examen de tu historial. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    examToDelete?.let { exam ->
                        examsEntities.find { it.id == exam.id }?.let { entity ->
                            patientViewModel.deleteExam(entity)
                        }
                    }
                    showDeleteDialog = false
                }) {
                    Text("Eliminar", color = Color.Red, fontWeight = FontWeight.Bold)
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
            // Header con Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(colorPrimary, Color(0xFF0EA5E9))))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 22.dp)
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
                        Text("Mis exámenes", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Resultados de laboratorio", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    // Logo PacienCare
                    Card(
                        modifier = Modifier.size(50.dp).shadow(4.dp, CircleShape),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Barra de Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    placeholder = { Text("Buscar examen o fecha...", fontSize = 14.sp) },
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

                Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (filteredExams.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron resultados", color = Color(0xFF64748B), fontSize = 14.sp)
                        }
                    } else {
                        filteredExams.forEach { exam ->
                            ExamDetailCard(
                                exam = exam,
                                onDelete = {
                                    examToDelete = exam
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun ExamDetailCard(exam: Exam, onDelete: () -> Unit) {
    val displayName = if (exam.name.isNotBlank() && !exam.name.equals("Examen de Laboratorio", ignoreCase = true)) exam.name else "Hemoglobina Glicosilada (HbA1c)"
    val displayIndications = if (exam.indications.isNotBlank()) exam.indications else "Ayuno de 8 horas"

    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Icono Tubo
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE2F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Science, null, tint = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(
                        text = if (exam.time.isNotBlank()) "${exam.date} · ${exam.time}" else exam.date,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(exam.statusColor.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(exam.status, fontSize = 11.sp, color = exam.statusColor, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp).padding(top = 4.dp)) {
                        Icon(Icons.Filled.Delete, null, tint = Color(0xFFEF4444).copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Requisito / Indicaciones (Ayuno)
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(modifier = Modifier.weight(1f))
                Text(exam.doctor, fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}
