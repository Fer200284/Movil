package com.sistema.paciencare.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisteredUserDto(
    @SerializedName(value = "id", alternate = ["Id", "user_id", "pk"])
    val id: Int? = null,

    @SerializedName(value = "username", alternate = ["Username", "user", "usuario"])
    val username: String? = null,

    @SerializedName(value = "first_name", alternate = ["firstName", "first_name_user", "nombre"])
    val firstName: String? = null,

    @SerializedName(value = "last_name", alternate = ["lastName", "last_name_user", "apellidos"])
    val lastName: String? = null,

    @SerializedName(value = "email", alternate = ["Email", "correo", "user_email"])
    val email: String? = null,

    @SerializedName(value = "rol", alternate = ["role", "Role", "Rol", "tipo_usuario", "TipoUsuario", "tipo", "user_role"])
    val rol: String? = null,

    @SerializedName(value = "is_staff", alternate = ["isStaff", "staff"])
    val isStaff: Boolean? = null,

    @SerializedName(value = "is_active", alternate = ["isActive", "active"])
    val isActive: Boolean? = null
)
