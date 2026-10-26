package com.sistema.paciencare.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MedicoDto(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName(value = "nombre", alternate = ["Nombre", "nombre_medico", "NombreMedico", "medico", "first_name"])
    val nombre: String? = null,

    @SerializedName(value = "apellidos", alternate = ["Apellidos", "apellidos_medico", "ApellidosMedico", "last_name"])
    val apellidos: String? = null,

    @SerializedName(value = "especialidad", alternate = ["Especialidad", "id_especialidad"])
    val especialidad: String? = null
) {
    fun getFullName(): String {
        val n = nombre?.trim() ?: ""
        val a = apellidos?.trim() ?: ""
        val full = "$n $a".trim()
        return if (full.isNotBlank()) {
            if (full.contains("Dr", ignoreCase = true)) full else "Dr. $full"
        } else "Dr. Carlos Ramírez"
    }
}
