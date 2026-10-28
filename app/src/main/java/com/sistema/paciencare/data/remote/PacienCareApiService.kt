package com.sistema.paciencare.data.remote

import com.sistema.paciencare.data.remote.dto.AtencionCronicoDto
import com.sistema.paciencare.data.remote.dto.ExamDto
import com.sistema.paciencare.data.remote.dto.LoginRequest
import com.sistema.paciencare.data.remote.dto.MedicamentoAtencionDto
import com.sistema.paciencare.data.remote.dto.MedicoDto
import com.sistema.paciencare.data.remote.dto.PacienteDto
import com.sistema.paciencare.data.remote.dto.RefreshTokenRequest
import com.sistema.paciencare.data.remote.dto.RegisterUserRequest
import com.sistema.paciencare.data.remote.dto.RegisteredUserDto
import com.sistema.paciencare.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PacienCareApiService {

    // --- AUTENTICACIÓN ---

    @POST("token/")
    suspend fun loginUser(
        @Body request: LoginRequest
    ): Response<TokenResponse>

    @POST("token/refresh/")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<TokenResponse>

    @POST("token/refresh/")
    fun refreshTokenSync(
        @Body request: RefreshTokenRequest
    ): retrofit2.Call<TokenResponse>

    // --- REGISTRO DE USUARIOS ---

    @POST("usuarios/")
    suspend fun registerUser(
        @Body request: RegisterUserRequest
    ): Response<RegisteredUserDto>

    @GET("usuarios/")
    suspend fun getUsuarios(
        @Header("Authorization") authHeader: String
    ): Response<List<RegisteredUserDto>>

    @GET("usuarios/resumen-movil/")
    suspend fun getResumenMovil(
        @Header("Authorization") authHeader: String
    ): Response<com.sistema.paciencare.data.remote.dto.ResumenMovilDto>

    // --- PACIENTES ---

    @GET("pacientes/")
    suspend fun getPacientes(
        @Header("Authorization") authHeader: String,
        @Query("usuario") usuarioId: Int? = null
    ): Response<List<PacienteDto>>

    @GET("catalogos/pacientes/")
    suspend fun getCatalogosPacientes(
        @Header("Authorization") authHeader: String
    ): Response<List<PacienteDto>>

    @GET("paciente/")
    suspend fun getPacienteSingular(
        @Header("Authorization") authHeader: String
    ): Response<List<PacienteDto>>

    @POST("pacientes/")
    suspend fun createPacienteRemote(
        @Header("Authorization") authHeader: String,
        @Body request: PacienteDto
    ): Response<PacienteDto>

    @PUT("pacientes/{id}/")
    suspend fun updatePacienteRemote(
        @Header("Authorization") authHeader: String,
        @Path("id") pacienteId: Int,
        @Body request: PacienteDto
    ): Response<PacienteDto>

    @PATCH("pacientes/{id}/")
    suspend fun patchPacienteRemote(
        @Header("Authorization") authHeader: String,
        @Path("id") pacienteId: Int,
        @Body request: PacienteDto
    ): Response<PacienteDto>

    // --- MÉDICOS ---

    @GET("medicos/")
    suspend fun getMedicosExact(
        @Header("Authorization") authHeader: String
    ): Response<List<MedicoDto>>

    @GET("medico/")
    suspend fun getMedicoAlt(
        @Header("Authorization") authHeader: String
    ): Response<List<MedicoDto>>

    @GET("catalogos/medicos/")
    suspend fun getCatalogosMedicos(
        @Header("Authorization") authHeader: String
    ): Response<List<MedicoDto>>

    // --- PACIENTE PATOLOGÍA ---

    @GET("paciente-patologia/")
    suspend fun getPacientePatologiaAlt(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.PacientePatologiaDto>>

    @GET("paciente_patologia/")
    suspend fun getPacientePatologiaUnderscore(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.PacientePatologiaDto>>

    @GET("catalogos/paciente-patologia/")
    suspend fun getCatalogosPacientePatologiaAlt(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.PacientePatologiaDto>>

    @GET("catalogos/paciente_patologia/")
    suspend fun getCatalogosPacientePatologiaUnderscore(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.PacientePatologiaDto>>

    // --- ATENCIONES CRÓNICAS / CITAS (Model: Atencion_Cronico) ---

    @GET("atencion_cronico/")
    suspend fun getAtencionCronicoUnderscore(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<AtencionCronicoDto>>

    @GET("atencion-cronico/")
    suspend fun getAtencionCronicoAlt(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<AtencionCronicoDto>>

    @GET("atenciones-cronicas/")
    suspend fun getAtencionesCronicas(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<AtencionCronicoDto>>

    @GET("catalogos/atencion-cronico/")
    suspend fun getCatalogosAtencionCronicoAlt(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<AtencionCronicoDto>>

    @GET("catalogos/atencion_cronico/")
    suspend fun getCatalogosAtencionCronicoUnderscore(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<AtencionCronicoDto>>

    // --- MEDICAMENTOS ---

    @GET("medicamentos/")
    suspend fun getMedicamentos(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<MedicamentoAtencionDto>>

    @GET("catalogos/medicamentos/")
    suspend fun getCatalogosMedicamentos(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<MedicamentoAtencionDto>>

    @GET("medicamentos/")
    suspend fun getMedicamentosCatalogExact(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.MedicamentoCatalogDto>>

    @GET("catalogos/medicamentos/")
    suspend fun getCatalogosMedicamentosCatalogExact(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.MedicamentoCatalogDto>>

    @GET("detalle-tratamiento/")
    suspend fun getDetallesTratamientoExact(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.DetalleTratamientoDto>>

    @GET("detalle_tratamiento/")
    suspend fun getDetalleTratamientoUnderscore(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.DetalleTratamientoDto>>

    @GET("catalogos/detalle-tratamiento/")
    suspend fun getCatalogosDetallesTratamientoExact(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.DetalleTratamientoDto>>

    @GET("catalogos/detalle_tratamiento/")
    suspend fun getCatalogosDetalleTratamientoUnderscore(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.DetalleTratamientoDto>>

    @GET("detalle_tratamiento/")
    suspend fun getDetalleTratamiento(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<MedicamentoAtencionDto>>

    @GET("tratamiento/")
    suspend fun getTratamientoExact(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.TratamientoDto>>

    @GET("tratamientos/")
    suspend fun getTratamientosAlt(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.TratamientoDto>>

    @GET("catalogos/tratamiento/")
    suspend fun getCatalogosTratamientoExact(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.TratamientoDto>>

    @GET("catalogos/tratamientos/")
    suspend fun getCatalogosTratamientosAlt(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.TratamientoDto>>

    @GET("tratamiento-cronico/")
    suspend fun getTratamientoCronicoAlt1(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.TratamientoDto>>

    @GET("tratamiento_cronico/")
    suspend fun getTratamientoCronicoAlt2(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.TratamientoDto>>

    // --- EXÁMENES ---

    @GET("solicitud-de-examenes/")
    suspend fun getSolicitudesDeExamenesExact(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.SolicitudExamenDto>>

    @GET("resultados-examenes/")
    suspend fun getResultadosExamenesExact(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.ResultadoExamenDto>>

    @GET("examenes-de-laboratorio/")
    suspend fun getExamenesLaboratorioCatalogExact(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.ExamenLaboratorioDto>>

    @GET("resultados-examenes/")
    suspend fun getResultadosExamenes(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<ExamDto>>

    @GET("resultado_examen/")
    suspend fun getResultadoExamenUnderscore(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<ExamDto>>

    @GET("solicitud_examenes/")
    suspend fun getSolicitudExamenesUnderscore(
        @Header("Authorization") authHeader: String,
        @Query("paciente") pacienteId: Int? = null
    ): Response<List<ExamDto>>

    // --- ANALYTICS / MONGODB FACT TABLE ---

    @POST("analytics/encuestas/")
    suspend fun postAnalyticsEncuesta(
        @Header("Authorization") authHeader: String,
        @Body request: com.sistema.paciencare.data.remote.dto.AnalyticsEncuestaRequest
    ): Response<Map<String, Any>>

    @GET("analytics/encuestas/")
    suspend fun getAnalyticsEncuestas(
        @Header("Authorization") authHeader: String
    ): Response<List<com.sistema.paciencare.data.remote.dto.AnalyticsEncuestaResponse>>

    @GET("analytics/encuestas/{id}/")
    suspend fun getAnalyticsEncuestaById(
        @Header("Authorization") authHeader: String,
        @Path("id") mongoId: String
    ): Response<com.sistema.paciencare.data.remote.dto.AnalyticsEncuestaResponse>

    @PATCH("analytics/encuestas/{id}/")
    suspend fun patchAnalyticsEncuesta(
        @Header("Authorization") authHeader: String,
        @Path("id") mongoId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<com.sistema.paciencare.data.remote.dto.AnalyticsEncuestaResponse>

    @DELETE("analytics/encuestas/{id}/")
    suspend fun deleteAnalyticsEncuesta(
        @Header("Authorization") authHeader: String,
        @Path("id") mongoId: String
    ): Response<Unit>
}
