package com.sistema.paciencare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sistema.paciencare.data.local.entities.AppointmentEntity
import com.sistema.paciencare.data.local.entities.ExamEntity
import com.sistema.paciencare.data.local.entities.MedicationEntity
import com.sistema.paciencare.data.local.entities.UserEntity
import com.sistema.paciencare.data.repository.PacienCareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.sistema.paciencare.data.local.SessionManager
import com.sistema.paciencare.data.remote.dto.RegisterUserRequest


class AuthViewModel(
    private val repository: PacienCareRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val INTERNAL_MANAGER_CODES = listOf("PC-GERENCIA-2026", "ADMIN-99", "PACI-BOSS")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loginResult = MutableStateFlow<Boolean?>(null)
    val loginResult: StateFlow<Boolean?> = _loginResult

    private val _loginErrorType = MutableStateFlow<String?>(null)
    val loginErrorType: StateFlow<String?> = _loginErrorType

    private val _loggedUser = MutableStateFlow<UserEntity?>(null)
    val loggedUser: StateFlow<UserEntity?> = _loggedUser

    fun validateAuthCode(code: String): Boolean {
        val clean = code.trim()
        return clean.isNotBlank() || INTERNAL_MANAGER_CODES.contains(clean)
    }

    fun registerUser(
        user: UserEntity,
        authCode: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {

            val cleanEmail = user.email.trim().lowercase()
            val cleanPassword = user.password.trim()

            if (cleanEmail.isEmpty() || cleanPassword.isEmpty()) {
                onError("El correo y la contraseña son obligatorios.")
                return@launch
            }

            val normalizedUser = user.copy(
                email = cleanEmail,
                password = cleanPassword
            )

            // Guardar localmente en Room DB
            repository.insertUser(normalizedUser)

            val nameParts = user.name.trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            val firstName = nameParts.firstOrNull() ?: user.name
            val lastName = nameParts.drop(1).joinToString(" ")

            val rawBirth = user.birthDate.trim()
            val formattedBirth = try {
                if (rawBirth.contains("/")) {
                    val sdfSlash = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val sdfIso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val parsed = sdfSlash.parse(rawBirth)
                    if (parsed != null) sdfIso.format(parsed) else rawBirth
                } else rawBirth
            } catch (_: Exception) {
                rawBirth
            }

            val request = RegisterUserRequest(
                username = cleanEmail,
                nombreCompleto = user.name.trim().ifBlank { null },
                firstName = firstName,
                lastName = lastName,
                cedula = user.cedula.ifBlank { null },
                fechaDeNacimiento = formattedBirth.ifBlank { null },
                celular = user.phone.ifBlank { null },
                email = cleanEmail,
                password = cleanPassword,
                rol = user.role,
                codigoAutorizacion = authCode.trim().ifBlank { null }
            )

            // Petición directa a la API en Azure
            val result = repository
                .getRemoteDataSource()
                .registerUser(request)

            result.onSuccess {
                onSuccess()
            }

            result.onFailure { error ->
                onError(error.message ?: "Error al comunicarse con la API remota.")
            }
        }
    }

    fun loginUser(
        email: String,
        passwordEntered: String,
        selectedRole: String
    ) {
        _loginResult.value = null
        _loginErrorType.value = null
        _loggedUser.value = null
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val cleanEmail = email.trim().lowercase()
                val cleanPassword = passwordEntered.trim()

                if (cleanEmail.isEmpty() || cleanPassword.isEmpty()) {
                    _loginErrorType.value = "CAMPOS_VACIOS"
                    _loginResult.value = false
                    return@launch
                }

                // Petición directa a la API en Azure (token/)
                val result = repository
                    .getRemoteDataSource()
                    .loginUser(
                        email = cleanEmail,
                        passwordEntered = cleanPassword,
                        role = selectedRole
                    )

                result.onSuccess { tokenResponse ->

                    if (tokenResponse == null) {
                        _loginErrorType.value = "RESPUESTA_VACIA"
                        _loginResult.value = false
                        return@onSuccess
                    }

                    // 1. Guardar JWT en la sesión
                    sessionManager.saveSession(
                        isLoggedIn = true,
                        name = cleanEmail,
                        isFemale = false,
                        accessToken = tokenResponse.access,
                        refreshToken = tokenResponse.refresh,
                        email = cleanEmail
                    )

                    // 2. Decodificar user_id del token JWT de SimpleJWT
                    val userId = com.sistema.paciencare.data.remote.JwtUtils.getUserIdFromToken(tokenResponse.access)

                    if (userId != null) {
                        // Consultar la tabla de usuarios y roles directamente de Azure DB (/api/usuarios/)
                        val usuariosList = repository.getRemoteDataSource().getUsuarios(tokenResponse.access).getOrNull() ?: emptyList()
                        val userInDb = usuariosList.find { cleanEmail.equals(it.email?.trim(), ignoreCase = true) || cleanEmail.equals(it.username?.trim(), ignoreCase = true) || it.id == userId }

                        // 3. Consultar /api/pacientes/?usuario={userId} enviando el token
                        val pacienteResult = repository
                            .getRemoteDataSource()
                            .getPacienteDelUsuario(userId, tokenResponse.access)

                        pacienteResult.onSuccess { pacientesList ->
                            val localUser = repository.getUserByEmailOnce(cleanEmail)

                            val userCedula = localUser?.cedula?.trim()?.lowercase() ?: ""
                            val emailPrefix = cleanEmail.substringBefore("@").lowercase()

                            // Pipeline de Búsqueda Multicriterio en Azure
                            val paciente = pacientesList.find { userCedula.length > 3 && !it.cedula.isNullOrBlank() && userCedula.equals(it.cedula?.trim(), ignoreCase = true) }
                                ?: pacientesList.find { !it.email.isNullOrBlank() && cleanEmail.equals(it.email?.trim(), ignoreCase = true) }
                                ?: pacientesList.find { !it.usuarioEmail.isNullOrBlank() && cleanEmail.equals(it.usuarioEmail?.trim(), ignoreCase = true) }
                                ?: pacientesList.find { userId > 0 && it.usuario == userId }
                                ?: pacientesList.find {
                                    val pNombre = "${it.nombre ?: ""} ${it.apellidos ?: ""}".trim().lowercase()
                                    pNombre.isNotBlank() && emailPrefix.isNotBlank() && (
                                        pNombre.contains(emailPrefix) ||
                                        emailPrefix.contains(pNombre.take(4)) ||
                                        (pNombre.split(" ").firstOrNull()?.let { fName -> fName.length >= 3 && emailPrefix.contains(fName) } == true)
                                    )
                                }
                                ?: if (pacientesList.size == 1) pacientesList.first() else null

                            // Leer el rol exacto de la columna 'rol'/'role' de Azure DB o por dominio administrativo
                            val dbRole = userInDb?.rol?.trim()
                            val isExplicitGerenciaDomain = cleanEmail.endsWith("@pacie.com", ignoreCase = true) ||
                                    cleanEmail.contains("geren", ignoreCase = true) ||
                                    cleanEmail.contains("admin", ignoreCase = true) ||
                                    cleanEmail.contains("boss", ignoreCase = true)

                            val isGerenciaInAzure = if (!dbRole.isNullOrBlank()) {
                                dbRole.equals("Gerencia", ignoreCase = true)
                            } else {
                                isExplicitGerenciaDomain
                            }

                            val isPatientInAzure = if (!dbRole.isNullOrBlank()) {
                                dbRole.equals("Paciente", ignoreCase = true)
                            } else {
                                !isGerenciaInAzure
                            }

                            // Validación Estricta de Roles basada en los Datos Reales de Azure DB
                            if (selectedRole.equals("Gerencia", ignoreCase = true) && isPatientInAzure) {
                                _loginErrorType.value = "ROL_PACIENTE_NO_GERENCIA"
                                _loginResult.value = false
                                return@onSuccess
                            }

                            if (selectedRole.equals("Paciente", ignoreCase = true) && isGerenciaInAzure) {
                                _loginErrorType.value = "ROL_GERENCIA_NO_PACIENTE"
                                _loginResult.value = false
                                return@onSuccess
                            }

                            val realRole = if (isGerenciaInAzure) "Gerencia" else "Paciente"

                            val fnPac = paciente?.nombre?.trim() ?: ""
                            val apPac = paciente?.apellidos?.trim() ?: ""
                            val nameFromPac = if (fnPac.isNotBlank() || apPac.isNotBlank()) "$fnPac $apPac".trim() else ""

                            val fnUser = userInDb?.firstName?.trim() ?: ""
                            val lnUser = userInDb?.lastName?.trim() ?: ""
                            val nameFromUserDb = if (fnUser.isNotBlank() || lnUser.isNotBlank()) "$fnUser $lnUser".trim() else ""

                            val fullName = when {
                                nameFromPac.isNotBlank() -> nameFromPac
                                nameFromUserDb.isNotBlank() -> nameFromUserDb
                                else -> cleanEmail.substringBefore("@").replace(".", " ").split(" ")
                                    .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } }
                            }

                            val existingLocalUser = repository.getUserByEmailOnce(cleanEmail)
                            val genderStr = when {
                                paciente?.sexo?.startsWith("F", ignoreCase = true) == true -> "Femenino"
                                paciente?.sexo?.startsWith("M", ignoreCase = true) == true -> "Masculino"
                                else -> paciente?.sexo?.ifBlank { null } ?: existingLocalUser?.gender ?: "No especificado"
                            }
                            val isFemale = genderStr.equals("Femenino", ignoreCase = true)

                            val userEntity = UserEntity(
                                email = cleanEmail,
                                name = fullName,
                                cedula = paciente?.cedula?.ifBlank { null } ?: existingLocalUser?.cedula ?: "",
                                birthDate = paciente?.fechaDeNacimiento?.ifBlank { null } ?: existingLocalUser?.birthDate ?: "",
                                gender = genderStr,
                                phone = paciente?.celular?.ifBlank { null } ?: existingLocalUser?.phone ?: "",
                                address = paciente?.direccion?.ifBlank { null } ?: existingLocalUser?.address ?: "",
                                role = realRole,
                                isFemale = isFemale,
                                password = cleanPassword
                            )

                            _loggedUser.value = userEntity
                            repository.insertUser(userEntity)

                            sessionManager.saveSession(
                                isLoggedIn = true,
                                name = fullName,
                                isFemale = isFemale,
                                role = realRole,
                                accessToken = tokenResponse.access,
                                refreshToken = tokenResponse.refresh,
                                email = cleanEmail
                            )

                            _loginResult.value = true
                        }

                        pacienteResult.onFailure {
                            val localUserDb = repository.getUserByEmailOnce(cleanEmail)
                            val dbRole = userInDb?.rol?.trim()

                            val isGerenciaAccount = if (!dbRole.isNullOrBlank()) {
                                dbRole.equals("Gerencia", ignoreCase = true)
                            } else {
                                cleanEmail.endsWith("@pacie.com", ignoreCase = true) ||
                                        cleanEmail.contains("geren", ignoreCase = true) ||
                                        cleanEmail.contains("admin", ignoreCase = true) ||
                                        localUserDb?.role?.equals("Gerencia", ignoreCase = true) == true
                            }

                            val isPatientAccount = if (!dbRole.isNullOrBlank()) {
                                dbRole.equals("Paciente", ignoreCase = true)
                            } else {
                                !isGerenciaAccount
                            }

                            if (selectedRole.equals("Gerencia", ignoreCase = true) && isPatientAccount) {
                                _loginErrorType.value = "ROL_PACIENTE_NO_GERENCIA"
                                _loginResult.value = false
                                return@onFailure
                            }

                            if (selectedRole.equals("Paciente", ignoreCase = true) && isGerenciaAccount) {
                                _loginErrorType.value = "ROL_GERENCIA_NO_PACIENTE"
                                _loginResult.value = false
                                return@onFailure
                            }

                            val realRole = if (isGerenciaAccount) "Gerencia" else "Paciente"

                            val localUser = UserEntity(
                                email = cleanEmail,
                                name = localUserDb?.name ?: cleanEmail.substringBefore("@").replace(".", " ")
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                                cedula = localUserDb?.cedula ?: "",
                                birthDate = localUserDb?.birthDate ?: "",
                                gender = localUserDb?.gender ?: "No especificado",
                                phone = localUserDb?.phone ?: "",
                                address = localUserDb?.address ?: "",
                                role = realRole,
                                isFemale = localUserDb?.isFemale ?: false,
                                password = cleanPassword
                            )
                            _loggedUser.value = localUser
                            repository.insertUser(localUser)
                            sessionManager.saveSession(
                                isLoggedIn = true,
                                name = localUser.name,
                                isFemale = false,
                                role = realRole,
                                accessToken = tokenResponse.access,
                                refreshToken = tokenResponse.refresh,
                                email = cleanEmail
                            )
                            _loginResult.value = true
                        }
                    } else {
                        val isGerenciaAccount = cleanEmail.contains("geren", ignoreCase = true) || 
                                cleanEmail.contains("admin", ignoreCase = true) || 
                                cleanEmail.contains("boss", ignoreCase = true)

                        if (selectedRole.equals("Gerencia", ignoreCase = true) && !isGerenciaAccount) {
                            _loginErrorType.value = "ROL_PACIENTE_NO_GERENCIA"
                            _loginResult.value = false
                            return@launch
                        }

                        if (selectedRole.equals("Paciente", ignoreCase = true) && isGerenciaAccount) {
                            _loginErrorType.value = "ROL_GERENCIA_NO_PACIENTE"
                            _loginResult.value = false
                            return@launch
                        }

                        val realRole = if (isGerenciaAccount) "Gerencia" else "Paciente"

                        val localUser = UserEntity(
                            email = cleanEmail,
                            name = cleanEmail.substringBefore("@").replace(".", " ")
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                            cedula = "",
                            birthDate = "",
                            gender = "No especificado",
                            phone = "",
                            address = "",
                            role = realRole,
                            isFemale = false,
                            password = cleanPassword
                        )
                        _loggedUser.value = localUser
                        repository.insertUser(localUser)
                        sessionManager.saveSession(
                            isLoggedIn = true,
                            name = localUser.name,
                            isFemale = false,
                            role = realRole,
                            accessToken = tokenResponse.access,
                            refreshToken = tokenResponse.refresh,
                            email = cleanEmail
                        )
                        _loginResult.value = true
                    }
                }

                result.onFailure { error ->
                    val errorMessage = error.message ?: ""
                    val isNetworkError = error is java.net.ConnectException ||
                            error is java.net.UnknownHostException ||
                            error is java.net.SocketTimeoutException ||
                            errorMessage.contains("HTTP 502", ignoreCase = true) ||
                            errorMessage.contains("HTTP 503", ignoreCase = true) ||
                            errorMessage.contains("Unable to resolve host", ignoreCase = true)

                    if (isNetworkError) {
                        // Modo fuera de línea (sin conexión a Internet)
                        val localUser = repository.getUserByEmailOnce(cleanEmail)
                        
                        if (localUser != null && localUser.password.isNotBlank() && localUser.password == cleanPassword) {
                            val isLocalPatient = localUser.role.equals("Paciente", ignoreCase = true)
                            val isLocalGerencia = localUser.role.equals("Gerencia", ignoreCase = true)

                            if (selectedRole.equals("Gerencia", ignoreCase = true) && isLocalPatient) {
                                _loginErrorType.value = "ROL_PACIENTE_NO_GERENCIA"
                                _loginResult.value = false
                                return@onFailure
                            }

                            if (selectedRole.equals("Paciente", ignoreCase = true) && isLocalGerencia) {
                                _loginErrorType.value = "ROL_GERENCIA_NO_PACIENTE"
                                _loginResult.value = false
                                return@onFailure
                            }

                            _loggedUser.value = localUser
                            sessionManager.saveSession(
                                isLoggedIn = true,
                                name = localUser.name,
                                isFemale = localUser.isFemale,
                                role = localUser.role,
                                email = cleanEmail
                            )
                            _loginResult.value = true
                        } else {
                            _loginErrorType.value = "CONTRASENA_INCORRECTA"
                            _loginResult.value = false
                        }
                    } else {
                        // Rechazo explícito de Azure (HTTP 401 / HTTP 400): las credenciales ingresadas son incorrectas
                        _loginErrorType.value = "CREDENCIALES_INCORRECTAS"
                        _loginResult.value = false
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyUserExists(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            val user = repository.getUserByEmailOnce(cleanEmail)
            onResult(user != null)
        }
    }

    fun resetLoginResult() {
        _loginResult.value = null
        _loggedUser.value = null
        _loginErrorType.value = null
    }
}
class AuthViewModelFactory(
    private val repository: PacienCareRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                repository = repository,
                sessionManager = sessionManager
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
