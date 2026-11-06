package com.sistema.paciencare.data.remote

import com.sistema.paciencare.data.local.SessionManager
import com.sistema.paciencare.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteDataSource(
    private val apiService: PacienCareApiService = RetrofitClient.apiService,
    private val sessionManager: SessionManager
) {

    // =========================================================
    // HELPER PARA RESPUESTAS ApiResponse<T>
    // =========================================================

    private suspend fun <T> safeApiCall(
        apiCall: suspend () -> retrofit2.Response<ApiResponse<T>>
    ): Result<T?> {

        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()

                if (response.isSuccessful) {

                    val body = response.body()

                    if (body?.success == true) {
                        Result.success(body.data)
                    } else {
                        Result.failure(
                            Exception(
                                body?.message
                                    ?: "Error desconocido en la API"
                            )
                        )
                    }

                } else {

                    val errorBody = response.errorBody()?.string()

                    Result.failure(
                        Exception(
                            "Error HTTP ${response.code()}: " +
                                    "${errorBody ?: response.message()}"
                        )
                    )
                }

            } catch (e: Exception) {

                Result.failure(e)
            }
        }
    }

    // =========================================================
    // HELPER PARA RESPUESTAS DIRECTAS
    // =========================================================

    private suspend fun <T> safeDirectApiCall(
        apiCall: suspend () -> retrofit2.Response<T>
    ): Result<T?> {

        return withContext(Dispatchers.IO) {

            try {

                val response = apiCall()

                if (response.isSuccessful) {

                    Result.success(response.body())

                } else {

                    val errorBody = response.errorBody()?.string()

                    Result.failure(
                        Exception(
                            "Error HTTP ${response.code()}: " +
                                    "${errorBody ?: response.message()}"
                        )
                    )
                }

            } catch (e: Exception) {

                Result.failure(e)
            }
        }
    }

    // =========================================================
    // AUTENTICACIÓN - LOGIN
    // =========================================================

    suspend fun loginUser(
        email: String,
        passwordEntered: String,
        role: String = "Paciente"
    ): Result<TokenResponse?> {

        return safeDirectApiCall {

            apiService.loginUser(
                LoginRequest(
                    username = email,
                    password = passwordEntered
                )
            )
        }
    }

    // =========================================================
    // AUTENTICACIÓN - REFRESH TOKEN
    // =========================================================

    suspend fun refreshAccessToken(): Result<TokenResponse?> {

        val refreshToken = sessionManager.getRefreshToken()

        if (refreshToken.isNullOrBlank()) {

            return Result.failure(
                Exception("No hay refresh token disponible.")
            )
        }

        return safeDirectApiCall {

            apiService.refreshToken(
                RefreshTokenRequest(
                    refresh = refreshToken
                )
            )
        }
    }

    // =========================================================
    // REGISTRO DE USUARIOS
    // =========================================================

    suspend fun registerUser(
        request: RegisterUserRequest
    ): Result<RegisteredUserDto?> {

        return safeDirectApiCall {

            apiService.registerUser(request)
        }
    }

    suspend fun getUsuarios(token: String): Result<List<RegisteredUserDto>> {
        return try {
            val response = apiService.getUsuarios("Bearer $token")
            if (response.isSuccessful) Result.success(response.body() ?: emptyList())
            else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getResumenMovil(token: String): Result<com.sistema.paciencare.data.remote.dto.ResumenMovilDto?> {
        return try {
            val response = apiService.getResumenMovil("Bearer $token")
            if (response.isSuccessful) Result.success(response.body())
            else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =========================================================
    // PACIENTES
    // =========================================================

    suspend fun getPacienteDelUsuario(userId: Int, token: String): Result<List<PacienteDto>> {
        return try {
            val list = mutableListOf<PacienteDto>()
            val response = apiService.getPacientes("Bearer $token", null)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                list.addAll(response.body()!!)
            }

            if (userId > 0) {
                val userResponse = apiService.getPacientes("Bearer $token", userId)
                if (userResponse.isSuccessful && !userResponse.body().isNullOrEmpty()) {
                    list.addAll(userResponse.body()!!)
                }
            }

            Result.success(list.distinctBy { it.id })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPacienteRemote(token: String, dto: PacienteDto): Result<PacienteDto?> {
        return try {
            val response = apiService.createPacienteRemote("Bearer $token", dto)
            if (response.isSuccessful) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePacienteRemote(token: String, pacienteId: Int, dto: PacienteDto): Result<PacienteDto?> {
        return try {
            val res1 = apiService.patchPacienteRemote("Bearer $token", pacienteId, dto)
            if (res1.isSuccessful) {
                Result.success(res1.body())
            } else {
                val res2 = apiService.updatePacienteRemote("Bearer $token", pacienteId, dto)
                if (res2.isSuccessful) Result.success(res2.body())
                else Result.failure(Exception("HTTP ${res1.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPacientePatologiaExact(token: String): Result<List<com.sistema.paciencare.data.remote.dto.PacientePatologiaDto>> {
        val list = mutableListOf<com.sistema.paciencare.data.remote.dto.PacientePatologiaDto>()
        runCatching {
            val res1 = apiService.getPacientePatologiaAlt("Bearer $token")
            if (res1.isSuccessful && !res1.body().isNullOrEmpty()) list.addAll(res1.body()!!)
        }
        runCatching {
            val res2 = apiService.getPacientePatologiaUnderscore("Bearer $token")
            if (res2.isSuccessful && !res2.body().isNullOrEmpty()) list.addAll(res2.body()!!)
        }
        runCatching {
            val res3 = apiService.getCatalogosPacientePatologiaAlt("Bearer $token")
            if (res3.isSuccessful && !res3.body().isNullOrEmpty()) list.addAll(res3.body()!!)
        }
        return Result.success(list.distinctBy { it.id })
    }

    suspend fun getMedicosExact(token: String): Result<List<com.sistema.paciencare.data.remote.dto.MedicoDto>> {
        val list = mutableListOf<com.sistema.paciencare.data.remote.dto.MedicoDto>()
        runCatching {
            val res1 = apiService.getMedicosExact("Bearer $token")
            if (res1.isSuccessful && !res1.body().isNullOrEmpty()) list.addAll(res1.body()!!)
        }
        runCatching {
            val res2 = apiService.getMedicoAlt("Bearer $token")
            if (res2.isSuccessful && !res2.body().isNullOrEmpty()) list.addAll(res2.body()!!)
        }
        return Result.success(list.distinctBy { it.id })
    }

    // =========================================================
    // ATENCIONES CRÓNICAS, MEDICAMENTOS Y EXÁMENES
    // =========================================================

    suspend fun getAllAtencionesCronicasExact(token: String): Result<List<AtencionCronicoDto>> {
        val list = mutableListOf<AtencionCronicoDto>()
        runCatching {
            val res1 = apiService.getAtencionCronicoAlt("Bearer $token", null)
            if (res1.isSuccessful && !res1.body().isNullOrEmpty()) list.addAll(res1.body()!!)
        }
        runCatching {
            val res2 = apiService.getAtencionCronicoUnderscore("Bearer $token", null)
            if (res2.isSuccessful && !res2.body().isNullOrEmpty()) list.addAll(res2.body()!!)
        }
        runCatching {
            val res3 = apiService.getAtencionesCronicas("Bearer $token", null)
            if (res3.isSuccessful && !res3.body().isNullOrEmpty()) list.addAll(res3.body()!!)
        }
        return Result.success(list.distinctBy { it.id })
    }

    suspend fun getAtencionesCronicas(token: String, pacienteId: Int? = null, userId: Int? = null): Result<List<AtencionCronicoDto>> {
        val list = mutableListOf<AtencionCronicoDto>()
        runCatching {
            val res1 = apiService.getAtencionCronicoAlt("Bearer $token", pacienteId)
            if (res1.isSuccessful && !res1.body().isNullOrEmpty()) list.addAll(res1.body()!!)
        }
        runCatching {
            val res2 = apiService.getAtencionCronicoUnderscore("Bearer $token", pacienteId)
            if (res2.isSuccessful && !res2.body().isNullOrEmpty()) list.addAll(res2.body()!!)
        }
        if (list.isNotEmpty()) {
            val filtered = list.filter { it.matchesPacienteOrUser(pacienteId, userId) }
            return Result.success(if (filtered.isNotEmpty()) filtered else list)
        }
        return Result.success(emptyList())
    }

    suspend fun getMedicamentos(token: String, pacienteId: Int? = null): Result<List<MedicamentoAtencionDto>> {
        val list = mutableListOf<MedicamentoAtencionDto>()
        runCatching {
            val altRes = apiService.getDetalleTratamiento("Bearer $token", pacienteId)
            if (altRes.isSuccessful && !altRes.body().isNullOrEmpty()) list.addAll(altRes.body()!!)
        }
        runCatching {
            val response = apiService.getMedicamentos("Bearer $token", pacienteId)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) list.addAll(response.body()!!)
        }
        return Result.success(list.distinctBy { it.id ?: it.nombre })
    }

    suspend fun getExamenes(token: String, pacienteId: Int? = null): Result<List<ExamDto>> {
        val list = mutableListOf<ExamDto>()
        runCatching {
            val res1 = apiService.getResultadosExamenes("Bearer $token", pacienteId)
            if (res1.isSuccessful && !res1.body().isNullOrEmpty()) list.addAll(res1.body()!!)
        }
        runCatching {
            val res2 = apiService.getResultadoExamenUnderscore("Bearer $token", pacienteId)
            if (res2.isSuccessful && !res2.body().isNullOrEmpty()) list.addAll(res2.body()!!)
        }
        runCatching {
            val res3 = apiService.getSolicitudExamenesUnderscore("Bearer $token", pacienteId)
            if (res3.isSuccessful && !res3.body().isNullOrEmpty()) list.addAll(res3.body()!!)
        }
        return Result.success(list.distinctBy { it.id ?: it.nombre })
    }

    // =========================================================
    // CONSULTAS EXACTAS DE MODELOS RELACIONALES DE LA API
    // =========================================================



    suspend fun getSolicitudesDeExamenesExact(token: String): Result<List<com.sistema.paciencare.data.remote.dto.SolicitudExamenDto>> {
        val list = mutableListOf<com.sistema.paciencare.data.remote.dto.SolicitudExamenDto>()
        runCatching {
            val res1 = apiService.getSolicitudesDeExamenesExact("Bearer $token")
            if (res1.isSuccessful && !res1.body().isNullOrEmpty()) list.addAll(res1.body()!!)
        }
        return Result.success(list.distinctBy { it.id })
    }

    suspend fun getResultadosExamenesExact(token: String): Result<List<com.sistema.paciencare.data.remote.dto.ResultadoExamenDto>> {
        val list = mutableListOf<com.sistema.paciencare.data.remote.dto.ResultadoExamenDto>()
        runCatching {
            val res1 = apiService.getResultadosExamenesExact("Bearer $token")
            if (res1.isSuccessful && !res1.body().isNullOrEmpty()) list.addAll(res1.body()!!)
        }
        return Result.success(list.distinctBy { it.id })
    }

    suspend fun getExamenesLaboratorioCatalogExact(token: String): Result<List<com.sistema.paciencare.data.remote.dto.ExamenLaboratorioDto>> {
        return try {
            val response = apiService.getExamenesLaboratorioCatalogExact("Bearer $token")
            if (response.isSuccessful) Result.success(response.body() ?: emptyList())
            else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getDetallesTratamientoExact(token: String): Result<List<com.sistema.paciencare.data.remote.dto.DetalleTratamientoDto>> {
        val list = mutableListOf<com.sistema.paciencare.data.remote.dto.DetalleTratamientoDto>()
        runCatching {
            val res1 = apiService.getDetallesTratamientoExact("Bearer $token")
            if (res1.isSuccessful && !res1.body().isNullOrEmpty()) list.addAll(res1.body()!!)
        }
        runCatching {
            val res2 = apiService.getDetalleTratamientoUnderscore("Bearer $token")
            if (res2.isSuccessful && !res2.body().isNullOrEmpty()) list.addAll(res2.body()!!)
        }
        runCatching {
            val res3 = apiService.getCatalogosDetallesTratamientoExact("Bearer $token")
            if (res3.isSuccessful && !res3.body().isNullOrEmpty()) list.addAll(res3.body()!!)
        }
        runCatching {
            val res4 = apiService.getCatalogosDetalleTratamientoUnderscore("Bearer $token")
            if (res4.isSuccessful && !res4.body().isNullOrEmpty()) list.addAll(res4.body()!!)
        }
        return Result.success(list.distinctBy { it.id })
    }

    suspend fun getMedicamentosCatalogExact(token: String): Result<List<com.sistema.paciencare.data.remote.dto.MedicamentoCatalogDto>> {
        val list = mutableListOf<com.sistema.paciencare.data.remote.dto.MedicamentoCatalogDto>()
        runCatching {
            val res1 = apiService.getMedicamentosCatalogExact("Bearer $token")
            if (res1.isSuccessful && !res1.body().isNullOrEmpty()) list.addAll(res1.body()!!)
        }
        runCatching {
            val res2 = apiService.getCatalogosMedicamentosCatalogExact("Bearer $token")
            if (res2.isSuccessful && !res2.body().isNullOrEmpty()) list.addAll(res2.body()!!)
        }
        return Result.success(list.distinctBy { it.id })
    }

    suspend fun getTratamientosExact(token: String): Result<List<com.sistema.paciencare.data.remote.dto.TratamientoDto>> {
        val list = mutableListOf<com.sistema.paciencare.data.remote.dto.TratamientoDto>()
        runCatching {
            val res1 = apiService.getTratamientoExact("Bearer $token")
            if (res1.isSuccessful && !res1.body().isNullOrEmpty()) list.addAll(res1.body()!!)
        }
        runCatching {
            val res2 = apiService.getTratamientosAlt("Bearer $token")
            if (res2.isSuccessful && !res2.body().isNullOrEmpty()) list.addAll(res2.body()!!)
        }
        runCatching {
            val res3 = apiService.getCatalogosTratamientoExact("Bearer $token")
            if (res3.isSuccessful && !res3.body().isNullOrEmpty()) list.addAll(res3.body()!!)
        }
        runCatching {
            val res4 = apiService.getTratamientoCronicoAlt1("Bearer $token")
            if (res4.isSuccessful && !res4.body().isNullOrEmpty()) list.addAll(res4.body()!!)
        }
        runCatching {
            val res5 = apiService.getTratamientoCronicoAlt2("Bearer $token")
            if (res5.isSuccessful && !res5.body().isNullOrEmpty()) list.addAll(res5.body()!!)
        }
        return Result.success(list.distinctBy { it.id })
    }

    suspend fun postAnalyticsEncuesta(
        token: String,
        request: com.sistema.paciencare.data.remote.dto.AnalyticsEncuestaRequest
    ): Result<Boolean> {
        return try {
            val res = apiService.postAnalyticsEncuesta("Bearer $token", request)
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAnalyticsEncuestas(token: String): Result<List<com.sistema.paciencare.data.remote.dto.AnalyticsEncuestaResponse>> {
        return try {
            val res = apiService.getAnalyticsEncuestas("Bearer $token")
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAnalyticsEncuesta(token: String, mongoId: String): Result<Boolean> {
        return try {
            val res = apiService.deleteAnalyticsEncuesta("Bearer $token", mongoId)
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postAnalyticsAtencion(
        token: String,
        request: com.sistema.paciencare.data.remote.dto.AnalyticsAtencionRequest
    ): Result<Boolean> {
        return try {
            val res = apiService.postAnalyticsAtencion("Bearer $token", request)
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAnalyticsAtenciones(token: String): Result<List<com.sistema.paciencare.data.remote.dto.AnalyticsAtencionRequest>> {
        return try {
            val res = apiService.getAnalyticsAtenciones("Bearer $token")
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
