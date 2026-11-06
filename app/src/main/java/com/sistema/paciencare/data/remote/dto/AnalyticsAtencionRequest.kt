package com.sistema.paciencare.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnalyticsAtencionRequest(
    @SerializedName("id_atencion")
    val idAtencion: Int, // ID operacional real de AtencionCronico

    @SerializedName("id_fecha")
    val idFecha: String, // Formato "AAAA-MM-DD"

    @SerializedName("id_paciente")
    val idPaciente: Int, // ID real de Paciente

    @SerializedName("id_medico")
    val idMedico: Int, // ID real de Medico

    @SerializedName("id_patologia")
    val idPatologia: Int, // ID real de Patologia

    @SerializedName("asistio_val")
    val asistioVal: Int = 1, // 1 = Asistió, 0 = Inasistencia, -1 = Cancelada

    @SerializedName("duracion_consulta_minutos")
    val duracionConsultaMinutos: Int = 25, // Entero no negativo

    @SerializedName("tiempo_espera_real_minutos")
    val tiempoEsperaRealMinutos: Int = 10, // Entero no negativo

    @SerializedName("receto_medicamento_flag")
    val recetoMedicamentoFlag: Int = 0, // 0 o 1

    @SerializedName("solicito_examen_flag")
    val solicitoExamenFlag: Int = 0 // 0 o 1
)
