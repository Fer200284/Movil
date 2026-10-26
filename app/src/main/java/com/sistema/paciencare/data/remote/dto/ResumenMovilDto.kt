package com.sistema.paciencare.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class ResumenMovilDto(
    @SerializedName(value = "proxima_cita", alternate = ["proximaCita", "cita_proxima", "cita", "proxima"])
    val proximaCita: ProximaCitaResumenDto? = null,

    @SerializedName(value = "medicamentos", alternate = ["medicamentos_indicados", "tratamiento", "meds"])
    val medicamentos: List<MedicamentoResumenDto>? = null,

    @SerializedName(value = "examenes_solicitados", alternate = ["examenes", "solicitudes_examenes", "examenes_laboratorio"])
    val examenesSolicitados: List<ExamenResumenDto>? = null
)

data class ProximaCitaResumenDto(
    @SerializedName(value = "id", alternate = ["id_cita", "id_atencion"])
    val id: Int? = null,

    @SerializedName(value = "fecha", alternate = ["fecha_proxima_cita", "fecha_cita", "date"])
    val fecha: String? = null,

    @SerializedName(value = "hora", alternate = ["time", "hora_cita"])
    val hora: String? = null,

    @SerializedName(value = "medico", alternate = ["doctor", "nombre_medico", "medico_nombre"])
    val medico: String? = null,

    @SerializedName(value = "patologia", alternate = ["motivo", "observaciones", "reason"])
    val patologia: String? = null
)

data class MedicamentoResumenDto(
    @SerializedName(value = "id", alternate = ["id_medicamento"])
    val id: Int? = null,

    @SerializedName(value = "nombre", alternate = ["nombre_medicamento", "medicamento"])
    val nombre: String? = null,

    @SerializedName(value = "presentacion", alternate = ["forma"])
    val presentacion: String? = null,

    @SerializedName(value = "concentracion", alternate = ["dosis"])
    val concentracion: String? = null,

    @SerializedName(value = "indicacion", alternate = ["indicaciones", "frecuencia", "frecuencia_indicada"])
    val indicacion: String? = null,

    @SerializedName(value = "cantidad", alternate = ["cantidad_entregada"])
    val cantidad: String? = null,

    @SerializedName(value = "estado", alternate = ["status"])
    val estado: String? = null
)

data class ExamenResumenDto(
    @SerializedName(value = "id", alternate = ["id_examen", "id_solicitud"])
    val id: Int? = null,

    @SerializedName(value = "nombre", alternate = ["nombre_examen", "examen", "tipo_examen"])
    val nombre: String? = null,

    @SerializedName(value = "indicaciones", alternate = ["indicacion", "observacion"])
    val indicaciones: String? = null,

    @SerializedName(value = "resultados", alternate = ["resultado", "interpretacion", "status"])
    val resultadosElement: JsonElement? = null,

    @SerializedName(value = "fecha", alternate = ["fecha_resultado", "fecha_de_envio"])
    val fecha: String? = null
) {
    val resultadoText: String?
        get() {
            val elem = resultadosElement ?: return null
            return try {
                if (elem.isJsonPrimitive) elem.asString
                else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("interpretacion") -> obj.get("interpretacion").asString
                        obj.has("resultado") -> obj.get("resultado").asString
                        obj.has("valor_resultado") -> obj.get("valor_resultado").asString
                        else -> null
                    }
                } else null
            } catch (_: Exception) { null }
        }
}
