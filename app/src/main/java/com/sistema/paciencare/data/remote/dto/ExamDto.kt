package com.sistema.paciencare.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class ExamDto(
    @SerializedName(value = "id", alternate = ["Id", "id_solicitud", "id_resultado", "id_examen"])
    val id: Int? = 0,

    @SerializedName(value = "paciente", alternate = ["IdPaciente", "id_paciente", "usuario", "paciente_id", "Paciente"])
    val pacienteElement: JsonElement? = null,

    @SerializedName(value = "doctor", alternate = ["Doctor", "medico", "IdMedico", "id_medico", "medico_solicitante", "nombre_medico"])
    val doctorElement: JsonElement? = null,

    @SerializedName(value = "Nombre_examen", alternate = ["nombre", "Nombre", "nombre_examen", "tipo_examen", "examen", "Examen", "descripcion", "examen_solicitado", "id_examen_de_laboratorio", "id_examen"])
    val nombreElement: JsonElement? = null,

    @SerializedName(value = "Interpretacion", alternate = ["interpretacion", "status", "estado", "Estado", "resultado", "Resultado", "observacion"])
    val interpretacion: String? = null,

    @SerializedName(value = "FechaResultado", alternate = ["fecha_resultado", "fecha", "Fecha", "fecha_de_envio", "FechaCreacion", "fecha_creacion", "fecha_solicitud"])
    val fecha: String? = null
) {
    val doctorName: String?
        get() {
            val elem = doctorElement ?: return null
            return try {
                if (elem.isJsonPrimitive) {
                    val str = elem.asString
                    if (str.isNotBlank() && !str.all { it.isDigit() }) str else null
                } else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("nombre") -> {
                            val n = obj.get("nombre").asString
                            val a = if (obj.has("apellidos")) obj.get("apellidos").asString else ""
                            "$n $a".trim()
                        }
                        obj.has("Nombre") -> {
                            val n = obj.get("Nombre").asString
                            val a = if (obj.has("Apellidos")) obj.get("Apellidos").asString else ""
                            "$n $a".trim()
                        }
                        else -> null
                    }
                } else null
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

    val idExamen: Int?
        get() {
            val elem = nombreElement ?: return null
            return try {
                if (elem.isJsonPrimitive && elem.asJsonPrimitive.isNumber) {
                    elem.asInt
                } else if (elem.isJsonPrimitive && elem.asString.all { it.isDigit() }) {
                    elem.asString.toIntOrNull()
                } else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("id") -> obj.get("id").asInt
                        obj.has("id_examen") -> obj.get("id_examen").asInt
                        obj.has("id_examen_de_laboratorio") -> obj.get("id_examen_de_laboratorio").asInt
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
                    val str = elem.asString
                    if (str.isNotBlank() && !str.all { it.isDigit() }) str else null
                } else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("nombre_examen") -> obj.get("nombre_examen").asString
                        obj.has("nombre") -> obj.get("nombre").asString
                        obj.has("Nombre") -> obj.get("Nombre").asString
                        obj.has("tipo_examen") -> obj.get("tipo_examen").asString
                        obj.has("examen") -> obj.get("examen").asString
                        obj.has("descripcion") -> obj.get("descripcion").asString
                        else -> null
                    }
                } else null
            } catch (_: Exception) {
                null
            }
        }
}
