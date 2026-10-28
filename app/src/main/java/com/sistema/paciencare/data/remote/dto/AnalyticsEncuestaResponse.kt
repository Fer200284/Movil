package com.sistema.paciencare.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnalyticsEncuestaResponse(
    @SerializedName(value = "id", alternate = ["_id", "mongo_id"])
    val id: String? = null,

    @SerializedName("id_encuesta")
    val idEncuesta: Int? = null,

    @SerializedName("id_fecha")
    val idFecha: String? = null,

    @SerializedName("id_paciente")
    val idPaciente: Int? = null,

    @SerializedName("id_patologia")
    val idPatologia: Int? = null,

    @SerializedName("puntuacion_general")
    val puntuacionGeneral: Int? = null,

    @SerializedName("score_atencion_medica")
    val scoreAtencionMedica: Int? = null,

    @SerializedName("tiempo_espera_minutos")
    val tiempoEsperaMinutos: Int? = null,

    @SerializedName("score_trato_personal")
    val scoreTratoPersonal: Int? = null,

    @SerializedName("score_consulta_doctor")
    val scoreConsultaDoctor: Int? = null,

    @SerializedName("resolucion_dudas_val")
    val resolucionDudasVal: Int? = null,

    @SerializedName(value = "comentarios", alternate = ["comentario", "comments", "sugerencias", "Comentario", "Comentarios"])
    val comentarios: String? = null
)
