package com.sistema.paciencare.data.repository

import com.sistema.paciencare.data.local.SessionManager
import com.sistema.paciencare.data.local.dao.PatientDao
import com.sistema.paciencare.data.local.entities.AppointmentEntity
import com.sistema.paciencare.data.local.entities.EvaluationEntity
import com.sistema.paciencare.data.local.entities.ExamEntity
import com.sistema.paciencare.data.local.entities.MedicationEntity
import com.sistema.paciencare.data.local.entities.UserEntity
import com.sistema.paciencare.data.remote.RemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow

class PacienCareRepository(
    private val patientDao: PatientDao,
    private val sessionManager: SessionManager
) {

    private val remoteDataSource =
        RemoteDataSource(
            sessionManager = sessionManager
        )

    private val _connectionStatus =
        kotlinx.coroutines.flow.MutableStateFlow(
            "Conectado localmente"
        )

    val connectionStatus:
            kotlinx.coroutines.flow.StateFlow<String> =
        _connectionStatus.asStateFlow()

    fun setConnectionMode(isRemoteOnline: Boolean) {
        _connectionStatus.value = if (isRemoteOnline) "Conectado remotamente" else "Conectado localmente"
    }

    // =========================================================
    // FUENTE DE DATOS REMOTA Y SESIÓN
    // =========================================================

    fun getRemoteDataSource(): RemoteDataSource =
        remoteDataSource

    fun getSessionManager(): SessionManager =
        sessionManager

    // =========================================================
    // LIMPIEZA DE CACHÉ LOCAL MULTIUSUARIO
    // =========================================================

    suspend fun clearDataForUser(email: String) {
        patientDao.clearAppointmentsForUser(email)
        patientDao.clearMedicationsForUser(email)
        patientDao.clearExamsForUser(email)
    }

    suspend fun clearAppointmentsForUser(email: String) {
        patientDao.clearAppointmentsForUser(email)
    }

    suspend fun clearMedicationsForUser(email: String) {
        patientDao.clearMedicationsForUser(email)
    }

    suspend fun clearExamsForUser(email: String) {
        patientDao.clearExamsForUser(email)
    }

    suspend fun clearAllCache() {
        patientDao.clearAllAppointments()
        patientDao.clearAllMedications()
        patientDao.clearAllExams()
        patientDao.clearAllEvaluations()
        sessionManager.clearAllDeletedItemBlacklists()
    }

    // =========================================================
    // USUARIO
    // =========================================================

    fun getUserByEmail(email: String) =
        patientDao.getUserByEmail(email)

    suspend fun getUserByEmailOnce(email: String) =
        patientDao.getUserByEmailOnce(email)

    suspend fun insertUser(user: UserEntity) =
        patientDao.insertUser(user)

    suspend fun updateUser(user: UserEntity) =
        patientDao.updateUser(user)

    suspend fun deleteUserByEmail(email: String) {
        patientDao.deleteUserByEmail(email)
        patientDao.clearAppointmentsForUser(email)
        patientDao.clearMedicationsForUser(email)
        patientDao.clearExamsForUser(email)
    }

    // =========================================================
    // CITAS
    // =========================================================

    fun getAppointmentsForUser(email: String): Flow<List<AppointmentEntity>> =
        patientDao.getAppointmentsForUser(email)

    suspend fun getAppointmentsForUserOnce(email: String): List<AppointmentEntity> =
        patientDao.getAppointmentsForUserOnce(email)

    val allAppointments: Flow<List<AppointmentEntity>> =
        patientDao.getAllAppointments()

    suspend fun insertAppointment(
        appointment: AppointmentEntity
    ) =
        patientDao.insertAppointment(appointment)

    suspend fun deleteAppointment(
        appointment: AppointmentEntity
    ) =
        patientDao.deleteAppointment(appointment)

    suspend fun clearAllAppointments() =
        patientDao.clearAllAppointments()

    // =========================================================
    // MEDICAMENTOS
    // =========================================================

    fun getMedicationsForUser(email: String): Flow<List<MedicationEntity>> =
        patientDao.getMedicationsForUser(email)

    fun getActiveMedicationsForUser(email: String): Flow<List<MedicationEntity>> =
        patientDao.getActiveMedicationsForUser(email)

    val allMedications: Flow<List<MedicationEntity>> =
        patientDao.getAllMedications()

    val activeMedications: Flow<List<MedicationEntity>> =
        patientDao.getActiveMedications()

    suspend fun insertMedication(
        medication: MedicationEntity
    ) =
        patientDao.insertMedication(medication)

    suspend fun deleteMedication(
        medication: MedicationEntity
    ) =
        patientDao.deleteMedication(medication)

    suspend fun updateMedication(
        medication: MedicationEntity
    ) =
        patientDao.updateMedication(medication)

    // =========================================================
    // EXÁMENES
    // =========================================================

    fun getExamsForUser(email: String): Flow<List<ExamEntity>> =
        patientDao.getExamsForUser(email)

    suspend fun getExamsForUserOnce(email: String): List<ExamEntity> =
        patientDao.getExamsForUserOnce(email)

    val allExams: Flow<List<ExamEntity>> =
        patientDao.getAllExams()

    suspend fun insertExam(
        exam: ExamEntity
    ) =
        patientDao.insertExam(exam)

    suspend fun deleteExam(
        exam: ExamEntity
    ) =
        patientDao.deleteExam(exam)

    // =========================================================
    // EVALUACIONES
    // =========================================================

    fun getEvaluationsForUser(email: String): Flow<List<EvaluationEntity>> =
        patientDao.getEvaluationsForUser(email)

    val allEvaluations: Flow<List<EvaluationEntity>> =
        patientDao.getAllEvaluations()

    suspend fun insertEvaluation(
        evaluation: EvaluationEntity
    ) =
        patientDao.insertEvaluation(evaluation)

    suspend fun deleteEvaluation(
        evaluation: EvaluationEntity
    ) =
        patientDao.deleteEvaluation(evaluation)

    suspend fun clearEvaluationsForUser(email: String) =
        patientDao.clearEvaluationsForUser(email)

    suspend fun clearAllEvaluations() =
        patientDao.clearAllEvaluations()
}
