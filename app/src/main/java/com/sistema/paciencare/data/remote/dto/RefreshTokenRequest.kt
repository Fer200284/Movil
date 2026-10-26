package com.sistema.paciencare.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RefreshTokenRequest(
    @SerializedName("refresh")
    val refresh: String
)
