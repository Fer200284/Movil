package com.sistema.paciencare.model

import androidx.compose.ui.graphics.Color

// --- ESTADOS DE NAVEGACIÓN ---
enum class ScreenState {
    Login,
    Register,
    ForgotPassword,
    PatientDashboard,
    Evaluation,
    MyAppointments,
    MyMedications,
    MyProfile,
    MyExams,
    ManagerDashboard
}

// --- MODELOS DE DATOS ---
data class Appointment(
    val id: Int = 0,
    val date: String,
    val time: String,
    val doctor: String,
    val reason: String,
    val status: String = "Confirmada" // Confirmada, Pendiente, Realizada, Cancelada
)

data class Medication(
    val id: Int = 0,
    val name: String,
    val dose: String,
    val frequency: String,
    val duration: String,
    val doctor: String = "Dr. Carlos Méndez",
    val status: String = "Activo" // Activo, Finalizado
)

data class Exam(
    val id: Int = 0,
    val name: String,
    val doctor: String,
    val date: String,
    val time: String = "",
    val indications: String = "",
    val status: String,
    val statusColor: Color
)
