package com.sistema.paciencare.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class PacienteDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName(value = "usuario", alternate = ["UsuarioId", "usuario_id", "Usuario", "user", "User"])
    val usuarioElement: JsonElement? = null,

    @SerializedName(value = "nombre", alternate = ["Nombre", "first_name"])
    val nombre: String?,

    @SerializedName(value = "apellidos", alternate = ["Apellidos", "last_name"])
    val apellidos: String?,

    @SerializedName(value = "sexo", alternate = ["Sexo", "gender"])
    val sexo: String?,

    @SerializedName(value = "fecha_de_nacimiento", alternate = ["Fecha_de_Nacimiento", "fechaDeNacimiento", "birthDate"])
    val fechaDeNacimiento: String?,

    @SerializedName(value = "celular", alternate = ["Celular", "phone", "telefono"])
    val celular: String?,

    @SerializedName(value = "cedula", alternate = ["Cedula"])
    val cedula: String?,

    @SerializedName(value = "direccion", alternate = ["Direccion", "address"])
    val direccion: String?,

    @SerializedName(value = "barrio", alternate = ["Barrio"])
    val barrio: String?,

    @SerializedName(value = "email", alternate = ["Email", "correo", "usuario_email", "user_email"])
    val email: String?,

    @SerializedName(value = "estado", alternate = ["Estado", "is_active"])
    val estado: Boolean?
) {
    val usuario: Int?
        get() {
            val elem = usuarioElement ?: return null
            return try {
                if (elem.isJsonPrimitive) {
                    val prim = elem.asJsonPrimitive
                    if (prim.isNumber) prim.asInt
                    else if (prim.isString) prim.asString.toIntOrNull()
                    else null
                } else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("id") -> obj.get("id").asInt
                        obj.has("Id") -> obj.get("Id").asInt
                        obj.has("usuario_id") -> obj.get("usuario_id").asInt
                        obj.has("pk") -> obj.get("pk").asInt
                        else -> null
                    }
                } else null
            } catch (_: Exception) {
                null
            }
        }

    val usuarioEmail: String?
        get() {
            if (!email.isNullOrBlank()) return email
            val elem = usuarioElement ?: return null
            return try {
                if (elem.isJsonPrimitive) {
                    val prim = elem.asJsonPrimitive
                    if (prim.isString) prim.asString else null
                } else if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    when {
                        obj.has("email") -> obj.get("email").asString
                        obj.has("username") -> obj.get("username").asString
                        obj.has("correo") -> obj.get("correo").asString
                        else -> null
                    }
                } else null
            } catch (_: Exception) {
                null
            }
        }
}
