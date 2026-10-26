package com.sistema.paciencare

import android.app.Application
import com.sistema.paciencare.data.local.PacienCareDatabase
import com.sistema.paciencare.data.local.SessionManager
import com.sistema.paciencare.data.remote.RetrofitClient
import com.sistema.paciencare.data.repository.PacienCareRepository

class PacienCareApplication : Application() {

    val sessionManager by lazy {
        SessionManager(this)
    }

    val database by lazy {
        PacienCareDatabase.getDatabase(this)
    }

    val repository by lazy {
        PacienCareRepository(
            patientDao = database.patientDao(),
            sessionManager = sessionManager
        )
    }

    override fun onCreate() {
        super.onCreate()

        RetrofitClient.initialize(this)
    }
}