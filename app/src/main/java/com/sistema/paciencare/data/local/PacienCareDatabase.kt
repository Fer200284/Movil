package com.sistema.paciencare.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sistema.paciencare.data.local.dao.PatientDao
import com.sistema.paciencare.data.local.entities.AppointmentEntity
import com.sistema.paciencare.data.local.entities.ExamEntity
import com.sistema.paciencare.data.local.entities.MedicationEntity

@Database(
    entities = [
        MedicationEntity::class,
        ExamEntity::class,
        AppointmentEntity::class,
        com.sistema.paciencare.data.local.entities.UserEntity::class,
        com.sistema.paciencare.data.local.entities.EvaluationEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class PacienCareDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao

    companion object {
        @Volatile
        private var INSTANCE: PacienCareDatabase? = null

        fun getDatabase(context: Context): PacienCareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PacienCareDatabase::class.java,
                    "paciencare_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
