package com.sistema.paciencare.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnalyticsEncuestaRequest(
    @SerializedName("id_encuesta")
    val idEncuesta: Int? = null,

    @SerializedName("id_fecha")
    val idFecha: String, // Formato ISO yyyy-MM-dd (ej. "2026-10-26")

    @SerializedName("id_paciente")
    val idPaciente: Int, // ID de la ficha Pacientes (ej. 102)

    @SerializedName("id_patologia")
    val idPatologia: Int, // ID de la patología crónica (ej. 12)

    @SerializedName("puntuacion_general")
    val puntuacionGeneral: Int, // 1 a 5 estrellas

    @SerializedName("score_atencion_medica")
    val scoreAtencionMedica: Int, // 1 a 5

    @SerializedName("tiempo_espera_minutos")
    val tiempoEsperaMinutos: Int, // minutos o escala (5, 15, 30, 45)

    @SerializedName("score_trato_personal")
    val scoreTratoPersonal: Int, // 1 a 5

    @SerializedName("score_consulta_doctor")
    val scoreConsultaDoctor: Int, // 1 a 5

    @SerializedName("resolucion_dudas_val")
    val resolucionDudasVal: Int, // 1 (Sí) o 0 (No/Parcial)

    @SerializedName("comentarios")
    val comentarios: String? = null,

    @SerializedName("comentario")
    val comentario: String? = null
)
