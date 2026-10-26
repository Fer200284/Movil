package com.sistema.paciencare.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisterUserRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName(value = "nombre_completo", alternate = ["nombreCompleto", "nombre_user"])
    val nombreCompleto: String? = null,

    @SerializedName("first_name")
    val firstName: String? = null,

    @SerializedName("last_name")
    val lastName: String? = null,

    @SerializedName("cedula")
    val cedula: String? = null,

    @SerializedName("fecha_de_nacimiento")
    val fechaDeNacimiento: String? = null,

    @SerializedName("celular")
    val celular: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("password")
    val password: String,

    @SerializedName("rol")
    val rol: String? = null,

    @SerializedName(value = "codigo_autorizacion", alternate = ["codigoAutorizacion", "codigo_invitacion", "auth_code"])
    val codigoAutorizacion: String? = null
)
