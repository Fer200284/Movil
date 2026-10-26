package com.sistema.paciencare.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class AtencionCronicoDto(
    @SerializedName(value = "id", alternate = ["Id", "id_atencion", "id_cita"])
    val id: Int,

    @SerializedName(value = "IdPaciente_Patologia", alternate = ["id_paciente_patologia", "paciente", "IdPaciente", "id_paciente", "paciente_id", "Paciente", "paciente_patologia"])
    val idPacienteElement: JsonElement? = null,

    @SerializedName(value = "IdMedico", alternate = ["id_medico", "medico", "doctor", "Doctor", "medico_atencion", "nombre_medico"])
    val doctorElement: JsonElement? = null,

    @SerializedName(value = "FechaAtencion", alternate = ["fecha_atencion", "Fecha_Atencion", "fechaAtencion", "fecha_creacion", "fecha", "Fecha"])
    val fechaAtencion: String?,

    @SerializedName(value = "FechaProximaCita", alternate = ["fecha_proxima_cita", "Fecha_Proxima_Cita", "fechaProximaCita", "fecha_proxima", "proxima_cita", "ProximaCita", "proximaCita", "Proxima_Cita", "Fecha_Proxima", "fechaProxima"])
    val fechaProximaCita: String?,

    @SerializedName(value = "hora", alternate = ["Hora", "hora_proxima_cita", "HoraProximaCita", "hora_cita", "time", "horaAtencion"])
    val hora: String? = null,

    @SerializedName(value = "Observaciones", alternate = ["observaciones", "observacion", "motivo", "Reason"])
    val observaciones: String?,

    @SerializedName(value = "PresionArterial", alternate = ["presion_arterial", "presionArterial"])
    val presionArterial: String?,

    @SerializedName(value = "Peso", alternate = ["peso"])
    val peso: Double?,

    @SerializedName(value = "Talla", alternate = ["talla"])
    val talla: Double?,

    @SerializedName(value = "asistio", alternate = ["Asistio", "asistencia", "Asistencia", "atendido", "Atendido", "estado", "Estado"])
    val asistio: String? = null
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

    val idMedicoInt: Int?
        get() {
            val elem = doctorElement ?: return null
            return try {
                if (elem.isJsonPrimitive) elem.asInt
                else if (elem.isJsonObject && elem.asJsonObject.has("id")) elem.asJsonObject.get("id").asInt
                else null
            } catch (_: Exception) { null }
        }

    val idPacientePatologia: Int?
        get() {
            val elem = idPacienteElement ?: return null
            return try {
                if (elem.isJsonPrimitive) {
                    elem.asInt
                } else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("id_paciente") -> obj.get("id_paciente").asInt
                        obj.has("paciente") -> obj.get("paciente").asInt
                        obj.has("id") -> obj.get("id").asInt
                        obj.has("Id") -> obj.get("Id").asInt
                        else -> null
                    }
                } else null
            } catch (_: Exception) {
                null
            }
        }

    val pacienteIdDeep: Int?
        get() {
            val elem = idPacienteElement ?: return null
            if (elem.isJsonPrimitive) return elem.asInt
            if (!elem.isJsonObject) return null
            return try {
                fun findPacienteIdInObject(obj: com.google.gson.JsonObject): Int? {
                    val keys = listOf("id_paciente", "paciente", "IdPaciente", "Paciente")
                    for (key in keys) {
                        if (obj.has(key)) {
                            val sub = obj.get(key)
                            if (sub.isJsonPrimitive) return sub.asInt
                            if (sub.isJsonObject) {
                                val subObj = sub.asJsonObject
                                if (subObj.has("id")) return subObj.get("id").asInt
                                if (subObj.has("id_paciente")) return subObj.get("id_paciente").asInt
                            }
                        }
                    }
                    for (entry in obj.entrySet()) {
                        if (entry.value.isJsonObject) {
                            val found = findPacienteIdInObject(entry.value.asJsonObject)
                            if (found != null) return found
                        }
                    }
                    return null
                }
                findPacienteIdInObject(elem.asJsonObject)
            } catch (_: Exception) { null }
        }

    fun matchesPacienteOrUser(pacienteId: Int?, userId: Int?): Boolean {
        if (pacienteId == null && userId == null) return false
        val elem = idPacienteElement ?: return false

        if (elem.isJsonPrimitive) {
            val valInt = try { elem.asInt } catch (_: Exception) { null }
            if (valInt != null) {
                if (pacienteId != null && valInt == pacienteId) return true
                if (userId != null && valInt == userId) return true
            }
        } else if (elem.isJsonObject) {
            val obj = elem.asJsonObject
            val idsInObj = mutableSetOf<Int>()

            fun extractInt(fieldName: String) {
                if (obj.has(fieldName)) {
                    val f = obj.get(fieldName)
                    if (f.isJsonPrimitive) {
                        try { idsInObj.add(f.asInt) } catch (_: Exception) {}
                    } else if (f.isJsonObject && f.asJsonObject.has("id")) {
                        try { idsInObj.add(f.asJsonObject.get("id").asInt) } catch (_: Exception) {}
                    }
                }
            }

            extractInt("id")
            extractInt("Id")
            extractInt("id_paciente")
            extractInt("paciente")
            extractInt("id_usuario")
            extractInt("usuario")
            extractInt("id_paciente_patologia")

            if (pacienteId != null && pacienteId in idsInObj) return true
            if (userId != null && userId in idsInObj) return true
        }
        return false
    }
}
