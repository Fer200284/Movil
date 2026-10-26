package com.sistema.paciencare.data.remote.dto

import com.google.gson.annotations.SerializedName

// --- RESUEST / RESPONSE GENERALES ---
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)


data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

data class UserDto(
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("cedula") val cedula: String,
    @SerializedName("birthDate") val birthDate: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("address") val address: String,
    @SerializedName("role") val role: String,
    @SerializedName("isFemale") val isFemale: Boolean
)

// --- CITAS ---
data class AppointmentDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String,
    @SerializedName("doctor") val doctor: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("status") val status: String
)

// --- MEDICAMENTOS ---
data class MedicationDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String,
    @SerializedName("dose") val dose: String,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("doctor") val doctor: String,
    @SerializedName("status") val status: String
)


