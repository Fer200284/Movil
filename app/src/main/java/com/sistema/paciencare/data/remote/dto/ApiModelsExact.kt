package com.sistema.paciencare.data.remote.dto

import com.google.gson.annotations.SerializedName

// --- RELACIÓN PACIENTE - PATOLOGÍA ---
data class PacientePatologiaDto(
    @SerializedName(value = "id", alternate = ["Id", "id_paciente_patologia"])
    val id: Int? = null,

    @SerializedName(value = "id_paciente", alternate = ["paciente", "IdPaciente", "paciente_id", "Paciente", "IdPaciente_Patologia"])
    val pacienteElement: com.google.gson.JsonElement? = null,

    @SerializedName(value = "id_patologia", alternate = ["patologia", "IdPatologia", "patologia_id", "Patologia"])
    val patologiaElement: com.google.gson.JsonElement? = null
) {
    val pacienteId: Int?
        get() {
            val elem = pacienteElement ?: return null
            return try {
                if (elem.isJsonPrimitive) elem.asInt
                else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("id") -> obj.get("id").asInt
                        obj.has("id_paciente") -> obj.get("id_paciente").asInt
                        else -> null
                    }
                } else null
            } catch (_: Exception) { null }
        }
}

// --- CATÁLOGO DE MEDICAMENTOS ---
data class MedicamentoCatalogDto(
    @SerializedName(value = "id", alternate = ["Id", "id_medicamento"])
    val id: Int? = null,

    @SerializedName(value = "nombre_medicamento", alternate = ["nombre", "Nombre", "medicamento", "Nombre_Medicamento"])
    val nombreMedicamento: String? = null,

    @SerializedName(value = "presentacion", alternate = ["Presentacion", "forma"])
    val presentacion: String? = null,

    @SerializedName(value = "concentracion", alternate = ["Concentracion", "dosis"])
    val concentracion: String? = null
)

// --- DETALLE DE TRATAMIENTO / PRESCRIPCIÓN ---
data class DetalleTratamientoDto(
    @SerializedName(value = "id", alternate = ["Id", "id_detalle"])
    val id: Int? = null,

    @SerializedName(value = "IdTratamiento", alternate = ["id_tratamiento", "id_atencion_cronico", "IdAtencionCronico", "id_atencion", "atencion", "tratamiento"])
    val idTratamientoElement: com.google.gson.JsonElement? = null,

    @SerializedName(value = "IdMedicamento", alternate = ["id_medicamento", "medicamento", "id_med"])
    val idMedicamentoElement: com.google.gson.JsonElement? = null,

    @SerializedName(value = "paciente", alternate = ["IdPaciente", "id_paciente", "paciente_id", "Paciente", "IdPaciente_Patologia"])
    val pacienteElement: com.google.gson.JsonElement? = null,

    @SerializedName(value = "CantidadEntregada", alternate = ["cantidad_entregada", "cantidad", "dosis", "Dosis", "concentracion"])
    val dosis: String? = null,

    @SerializedName(value = "Indicacion", alternate = ["indicacion", "Indicaciones", "indicaciones", "frecuencia", "Frecuencia", "instrucciones"])
    val frecuencia: String? = null
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

    val idMedicamento: Int?
        get() {
            val elem = idMedicamentoElement ?: return null
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
                if (elem.isJsonPrimitive) elem.asInt
                else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("id") -> obj.get("id").asInt
                        obj.has("id_paciente") -> obj.get("id_paciente").asInt
                        else -> null
                    }
                } else null
            } catch (_: Exception) { null }
        }

    val pacienteIdDeep: Int?
        get() {
            pacienteId?.let { return it }
            val elem = idTratamientoElement ?: return null
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

    val idAtencionCronicoFromTratamiento: Int?
        get() {
            val elem = idTratamientoElement ?: return null
            return try {
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("id_atencion_cronico") -> {
                            val f = obj.get("id_atencion_cronico")
                            if (f.isJsonPrimitive) f.asInt
                            else if (f.isJsonObject && f.asJsonObject.has("id")) f.asJsonObject.get("id").asInt
                            else null
                        }
                        obj.has("id_atencion") -> {
                            val f = obj.get("id_atencion")
                            if (f.isJsonPrimitive) f.asInt else null
                        }
                        else -> null
                    }
                } else null
            } catch (_: Exception) { null }
        }

    val nombreMedicamento: String?
        get() {
            val elem = idMedicamentoElement ?: return null
            return try {
                if (elem.isJsonPrimitive) null
                else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("nombre_medicamento") -> obj.get("nombre_medicamento").asString
                        obj.has("nombre") -> obj.get("nombre").asString
                        obj.has("Nombre") -> obj.get("Nombre").asString
                        obj.has("medicamento") -> obj.get("medicamento").asString
                        else -> null
                    }
                } else null
            } catch (_: Exception) { null }
        }
}

// --- SOLICITUD DE EXÁMENES ---
data class SolicitudExamenDto(
    @SerializedName(value = "id", alternate = ["Id", "id_solicitud"])
    val id: Int? = null,

    @SerializedName(value = "Id_Examen_De_Laboratorio", alternate = ["id_examen_de_laboratorio", "id_examen", "examen", "id_tipo_examen"])
    val idExamenDeLaboratorioElement: com.google.gson.JsonElement? = null,

    @SerializedName(value = "Id_Atencion_Cronico", alternate = ["id_atencion_cronico", "id_atencion", "atencion", "IdAtencionCronico", "IdTratamiento", "id_tratamiento"])
    val idAtencionCronicoElement: com.google.gson.JsonElement? = null,

    @SerializedName(value = "Indicaciones", alternate = ["indicaciones", "observacion"])
    val indicaciones: String? = null,

    @SerializedName(value = "Fecha_De_Envio", alternate = ["fecha_de_envio", "fecha", "Fecha", "fecha_solicitud"])
    val fechaDeEnvio: String? = null
) {
    val idExamenDeLaboratorio: Int?
        get() {
            val elem = idExamenDeLaboratorioElement ?: return null
            return try {
                if (elem.isJsonPrimitive) elem.asInt
                else if (elem.isJsonObject && elem.asJsonObject.has("id")) elem.asJsonObject.get("id").asInt
                else null
            } catch (_: Exception) { null }
        }

    val nombreExamenDirect: String?
        get() {
            val elem = idExamenDeLaboratorioElement ?: return null
            return try {
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("nombre_examen") -> obj.get("nombre_examen").asString
                        obj.has("nombre") -> obj.get("nombre").asString
                        obj.has("Nombre") -> obj.get("Nombre").asString
                        else -> null
                    }
                } else null
            } catch (_: Exception) { null }
        }

    val idAtencionCronico: Int?
        get() {
            val elem = idAtencionCronicoElement ?: return null
            return try {
                if (elem.isJsonPrimitive) elem.asInt
                else if (elem.isJsonObject && elem.asJsonObject.has("id")) elem.asJsonObject.get("id").asInt
                else null
            } catch (_: Exception) { null }
        }
}

// --- RESULTADO DE EXÁMENES ---
data class ResultadoExamenDto(
    @SerializedName(value = "id", alternate = ["Id", "id_resultado"])
    val id: Int? = null,

    @SerializedName(value = "Id_Solicitud_Examen", alternate = ["id_solicitud_examen", "id_solicitud", "solicitud_examen"])
    val idSolicitudExamenElement: com.google.gson.JsonElement? = null,

    @SerializedName(value = "ValorResultado", alternate = ["valor_resultado", "valor", "resultado"])
    val valorResultado: String? = null,

    @SerializedName(value = "Interpretacion", alternate = ["interpretacion", "estado", "observacion"])
    val interpretacion: String? = null,

    @SerializedName(value = "FechaResultado", alternate = ["fecha_resultado", "fecha", "Fecha", "fecha_creacion"])
    val fechaResultado: String? = null
) {
    val idSolicitudExamen: Int?
        get() {
            val elem = idSolicitudExamenElement ?: return null
            return try {
                if (elem.isJsonPrimitive) elem.asInt
                else if (elem.isJsonObject && elem.asJsonObject.has("id")) elem.asJsonObject.get("id").asInt
                else null
            } catch (_: Exception) { null }
        }
}

// --- CATÁLOGO DE EXÁMENES DE LABORATORIO ---
data class ExamenLaboratorioDto(
    @SerializedName(value = "id", alternate = ["Id", "id_examen"])
    val id: Int? = null,

    @SerializedName(value = "nombre_examen", alternate = ["nombre", "Nombre", "tipo_examen", "descripcion"])
    val nombreExamen: String? = null
)

// --- TRATAMIENTO (TABLA INTERMEDIA ATENCIÓN -> DETALLE) ---
data class TratamientoDto(
    @SerializedName(value = "id", alternate = ["Id", "id_tratamiento"])
    val id: Int? = null,

    @SerializedName(value = "IdAtencionCronico", alternate = ["id_atencion_cronico", "Id_Atencion_Cronico", "id_atencion", "atencion", "IdAtencion"])
    val idAtencionCronicoElement: com.google.gson.JsonElement? = null,

    @SerializedName(value = "FechaTratamiento", alternate = ["fecha_tratamiento", "fecha"])
    val fechaTratamiento: String? = null,

    @SerializedName(value = "Observaciones", alternate = ["observaciones", "observacion"])
    val observaciones: String? = null
) {
    val idAtencionCronico: Int?
        get() {
            val elem = idAtencionCronicoElement ?: return null
            return try {
                if (elem.isJsonPrimitive) elem.asInt
                else if (elem.isJsonObject && elem.asJsonObject.has("id")) elem.asJsonObject.get("id").asInt
                else null
            } catch (_: Exception) { null }
        }
}
