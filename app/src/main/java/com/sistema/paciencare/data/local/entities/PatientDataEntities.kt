package com.sistema.paciencare.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications", primaryKeys = ["userEmail", "id"])
data class MedicationEntity(
    val id: Int,
    val userEmail: String = "",
    val name: String,
    val dose: String,
    val frequency: String,
    val duration: String,
    val doctor: String,
    val status: String
)

@Entity(tableName = "exams", primaryKeys = ["userEmail", "id"])
data class ExamEntity(
    val id: Int,
    val userEmail: String = "",
    val name: String,
    val doctor: String,
    val date: String,
    val time: String = "",
    val indications: String = "",
    val status: String,
    val statusColor: Int // Guardamos el ARGB como Int
)

@Entity(tableName = "appointments", primaryKeys = ["userEmail", "id"])
data class AppointmentEntity(
    val id: Int,
    val userEmail: String = "",
    val date: String,
    val time: String,
    val doctor: String,
    val reason: String,
    val status: String
)

@Entity(tableName = "evaluations")
data class EvaluationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String = "",
    val rating1: Int,
    val rating2: Int,
    val rating3: Int,
    val rating4: Int,
    val rating5: Int,
    val overallRating: Float,
    val comments: String,
    val date: String
)
