package com.sistema.paciencare.data.local.dao

import androidx.room.*
import com.sistema.paciencare.data.local.entities.AppointmentEntity
import com.sistema.paciencare.data.local.entities.EvaluationEntity
import com.sistema.paciencare.data.local.entities.ExamEntity
import com.sistema.paciencare.data.local.entities.MedicationEntity
import com.sistema.paciencare.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    // --- USUARIO ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    fun getUserByEmail(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmailOnce(email: String): UserEntity?

    @Query("DELETE FROM users WHERE LOWER(email) = LOWER(:email)")
    suspend fun deleteUserByEmail(email: String)

    // --- CITAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Query("SELECT * FROM appointments WHERE LOWER(userEmail) = LOWER(:email) ORDER BY id DESC")
    fun getAppointmentsForUser(email: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE LOWER(userEmail) = LOWER(:email) ORDER BY id DESC")
    suspend fun getAppointmentsForUserOnce(email: String): List<AppointmentEntity>

    @Query("SELECT * FROM appointments ORDER BY id DESC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments WHERE LOWER(userEmail) = LOWER(:email)")
    suspend fun clearAppointmentsForUser(email: String)

    @Query("DELETE FROM appointments")
    suspend fun clearAllAppointments()

    // --- MEDICAMENTOS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity)

    @Query("SELECT * FROM medications WHERE (LOWER(status) LIKE '%activo%' OR status = '') AND LOWER(userEmail) = LOWER(:email)")
    fun getActiveMedicationsForUser(email: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE LOWER(userEmail) = LOWER(:email)")
    fun getMedicationsForUser(email: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE status = 'Activo'")
    fun getActiveMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE LOWER(userEmail) = LOWER(:email)")
    suspend fun clearMedicationsForUser(email: String)

    @Query("DELETE FROM medications")
    suspend fun clearAllMedications()

    // --- EXÁMENES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Query("SELECT * FROM exams WHERE LOWER(userEmail) = LOWER(:email) ORDER BY date DESC")
    fun getExamsForUser(email: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE LOWER(userEmail) = LOWER(:email) ORDER BY date DESC")
    suspend fun getExamsForUserOnce(email: String): List<ExamEntity>

    @Query("SELECT * FROM exams ORDER BY date DESC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE LOWER(userEmail) = LOWER(:email)")
    suspend fun clearExamsForUser(email: String)

    @Query("DELETE FROM exams")
    suspend fun clearAllExams()

    // --- EVALUACIONES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: EvaluationEntity)

    @Query("SELECT * FROM evaluations WHERE LOWER(userEmail) = LOWER(:email) ORDER BY id DESC")
    fun getEvaluationsForUser(email: String): Flow<List<EvaluationEntity>>

    @Query("SELECT * FROM evaluations ORDER BY id DESC")
    fun getAllEvaluations(): Flow<List<EvaluationEntity>>

    @Delete
    suspend fun deleteEvaluation(evaluation: EvaluationEntity)

    @Query("DELETE FROM evaluations WHERE LOWER(userEmail) = LOWER(:email)")
    suspend fun clearEvaluationsForUser(email: String)

    @Query("DELETE FROM evaluations")
    suspend fun clearAllEvaluations()
}
