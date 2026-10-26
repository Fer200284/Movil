package com.sistema.paciencare.ui.patient

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
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
import com.sistema.paciencare.model.Medication
import com.sistema.paciencare.viewmodel.PatientViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(patientViewModel: PatientViewModel, onBack: () -> Unit) {
    val colorPrimary = Color(0xFF0284C7)
    val colorBg = Color(0xFFF1F5F9)
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Refrescar datos en tiempo real al ingresar a Mis Medicamentos
    LaunchedEffect(Unit) {
        patientViewModel.refreshData()
    }

    // Lista observada desde el ViewModel
    val medicationsEntities by patientViewModel.allMedications.collectAsState()

    // Mapeo a modelo de UI (Solo los activos sin duplicados)
    val medicationsList = medicationsEntities
        .filter { it.status == "Activo" }
        .distinctBy { it.name.trim().lowercase() }
        .map { Medication(it.id, it.name, it.dose, it.frequency, it.duration, it.doctor, it.status) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var medicationToDelete by remember { mutableStateOf<Medication?>(null) }

    // Filtrado dinámico
    val filteredMeds = medicationsList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.doctor.contains(searchQuery, ignoreCase = true)
    }

    var showActiveMedAlert by remember { mutableStateOf(false) }

    if (showActiveMedAlert) {
        AlertDialog(
            onDismissRequest = { showActiveMedAlert = false },
            title = { Text("Tratamiento Activo", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
            text = { Text("Los medicamentos con tratamiento activo no se podrán eliminar a menos que el médico no lo siga recetando.", fontSize = 14.sp, color = Color(0xFF334155)) },
            confirmButton = {
                TextButton(onClick = { showActiveMedAlert = false }) {
                    Text("Entendido", color = colorPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showDeleteDialog && medicationToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar medicamento?", fontWeight = FontWeight.Bold) },
            text = { Text("Se borrará este registro de tu tratamiento. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    medicationToDelete?.let { med ->
                        medicationsEntities.find { it.id == med.id }?.let { entity ->
                            patientViewModel.deleteMedication(entity)
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
                        Text("Mis medicamentos", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Tratamientos actuales", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
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
                    placeholder = { Text("Buscar medicamento...", fontSize = 14.sp) },
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

                Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                    if (filteredMeds.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay medicamentos activos", color = Color(0xFF64748B), fontSize = 14.sp)
                        }
                    } else {
                        filteredMeds.forEach { med ->
                            MedicationDetailCard(
                                med = med,
                                onDelete = {
                                    if (med.status.equals("Activo", ignoreCase = true)) {
                                        showActiveMedAlert = true
                                    } else {
                                        medicationToDelete = med
                                        showDeleteDialog = true
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun MedicationDetailCard(med: Medication, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF0FDF4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Medication, null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(med.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Médico responsable: ${med.doctor}", fontSize = 13.sp, color = Color(0xFF64748B))
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0FDF4)).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Activo", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFF1F5F9))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailInfoRow("Dosis:", med.dose, Color(0xFF0284C7))
                    DetailInfoRow("Frecuencia:", med.frequency, Color(0xFF10B981))
                    DetailInfoRow("Duración:", med.duration, Color(0xFFF59E0B))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo Paci en el espacio en blanco de la derecha (Círculo blanco limpio)
                    Card(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(3.dp, CircleShape),
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

                    Spacer(modifier = Modifier.height(10.dp))

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, "Eliminar", tint = Color(0xFFEF4444).copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailInfoRow(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
    }
}
