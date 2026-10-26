package com.sistema.paciencare.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class MedicamentoAtencionDto(
    @SerializedName(value = "id", alternate = ["Id", "id_medicamento", "id_tratamiento", "id_detalle"])
    val id: Int?,

    @SerializedName(value = "paciente", alternate = ["IdPaciente", "id_paciente", "usuario", "paciente_id", "Paciente"])
    val pacienteElement: JsonElement? = null,

    @SerializedName(value = "doctor", alternate = ["Doctor", "medico", "IdMedico", "id_medico", "medico_prescribe", "nombre_medico"])
    val doctorElement: JsonElement? = null,

    @SerializedName(value = "nombre", alternate = ["Nombre", "nombre_medicamento", "medicamento", "Nombre_Medicamento", "descripcion", "Descripcion"])
    val nombreElement: JsonElement? = null,

    @SerializedName(value = "id_tratamiento", alternate = ["id_atencion_cronico", "IdAtencionCronico", "id_atencion", "atencion", "tratamiento"])
    val idTratamientoElement: JsonElement? = null,

    @SerializedName(value = "forma", alternate = ["Forma", "Indicacion", "indicacion", "Indicaciones", "indicaciones", "frecuencia", "Frecuencia", "forma_farmaceutica"])
    val forma: String?,

    @SerializedName(value = "presentacion", alternate = ["Presentacion", "CantidadEntregada", "cantidad_entregada", "dosis", "Dosis", "Concentracion", "concentracion"])
    val presentacion: String?
) {
    val idTratamiento: Int?
        get() {
            val elem = idTratamientoElement ?: return null
            return try {
                if (elem.isJsonPrimitive) elem.asInt
                else if (elem.isJsonObject && elem.asJsonObject.has("id")) elem.asJsonObject.get("id").asInt
                else null
            } catch (_: Exception) { null }
        }

    val pacienteId: Int?
        get() {
            val elem = pacienteElement ?: return null
            return try {
                if (elem.isJsonPrimitive) {
                    elem.asInt
                } else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("id") -> obj.get("id").asInt
                        obj.has("Id") -> obj.get("Id").asInt
                        obj.has("id_paciente") -> obj.get("id_paciente").asInt
                        obj.has("paciente") -> obj.get("paciente").asInt
                        else -> null
                    }
                } else null
            } catch (_: Exception) {
                null
            }
        }

    val nombre: String?
        get() {
            val elem = nombreElement ?: return null
            return try {
                if (elem.isJsonPrimitive) {
                    elem.asString
                } else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("nombre") -> obj.get("nombre").asString
                        obj.has("Nombre") -> obj.get("Nombre").asString
                        obj.has("nombre_medicamento") -> obj.get("nombre_medicamento").asString
                        obj.has("medicamento") -> obj.get("medicamento").asString
                        obj.has("descripcion") -> obj.get("descripcion").asString
                        else -> null
                    }
                } else null
            } catch (_: Exception) {
                null
            }
        }
}
