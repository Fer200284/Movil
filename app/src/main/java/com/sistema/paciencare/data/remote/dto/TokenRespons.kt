package com.sistema.paciencare.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("access")
    val access: String,

    @SerializedName("refresh")
    val refresh: String
)