package com.sistema.paciencare.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val cedula: String,
    val birthDate: String,
    val gender: String,
    val phone: String,
    val address: String,
    val role: String,
    val isFemale: Boolean,
    val password: String
)
