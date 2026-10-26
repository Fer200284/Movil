package com.sistema.paciencare.viewmodel

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sistema.paciencare.data.local.entities.AppointmentEntity
import com.sistema.paciencare.data.local.entities.EvaluationEntity
import com.sistema.paciencare.data.local.entities.ExamEntity
import com.sistema.paciencare.data.local.entities.MedicationEntity
import com.sistema.paciencare.data.local.entities.UserEntity
import com.sistema.paciencare.data.remote.JwtUtils
import com.sistema.paciencare.data.remote.NetworkObserver
import com.sistema.paciencare.data.remote.dto.*
import com.sistema.paciencare.data.repository.PacienCareRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class PatientViewModel(private val repository: PacienCareRepository) : ViewModel() {

    // --- ESTADO DE CONEXIÓN ---
    val connectionStatus: StateFlow<String> = repository.connectionStatus

    fun observeNetwork(context: Context) {
        viewModelScope.launch {
            NetworkObserver(context).observe().collect { isOnline ->
                repository.setConnectionMode(isOnline)
            }
        }
    }

    // --- USUARIO ---
    private val _currentUserEmail = MutableStateFlow(repository.getSessionManager().getUserEmail() ?: "")

    init {
        val saved = repository.getSessionManager().getUserEmail() ?: ""
        if (saved.isNotBlank()) {
            loadUser(saved)
        }
    }

    val currentUser: StateFlow<UserEntity?> = _currentUserEmail
        .flatMapLatest { email ->
            if (email.isEmpty()) flowOf(null)
            else repository.getUserByEmail(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var pollingJob: Job? = null

    fun startPeriodicSync() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(15000L)
                if (_currentUserEmail.value.isNotBlank()) {
                    syncRemoteData()
                    syncManagerData()
                }
            }
        }
    }

    fun stopPeriodicSync() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun clearActiveUser() {
        stopPeriodicSync()
        _currentUserEmail.value = ""
    }

    fun loadUser(email: String) {
        val clean = email.trim().lowercase()
        _currentUserEmail.value = clean
        syncRemoteData(isInitialSync = true)
        startPeriodicSync()
    }

    fun refreshData() {
        val current = _currentUserEmail.value
        if (current.isNotEmpty()) {
            isSyncing = false
            syncRemoteData(isInitialSync = false)
            syncManagerData()
        }
    }

    fun wipeLocalDataAndRefetchFromApi() = viewModelScope.launch {
        repository.clearAllCache()
        isSyncing = false
        syncRemoteData(isInitialSync = true)
        syncManagerData()
    }

    private var isSyncing = false

    fun syncRemoteData(isInitialSync: Boolean = false) {
        if (isSyncing) return
        val token = repository.getSessionManager().getAccessToken()
        val activeEmail = _currentUserEmail.value
        if (activeEmail.isNotBlank()) {
            isSyncing = true
            viewModelScope.launch {
                try {
                    withTimeoutOrNull(15000L.milliseconds) {
                        val userObj = repository.getUserByEmailOnce(activeEmail)

                        if (token.isNullOrBlank()) {
                            return@withTimeoutOrNull
                        }

                        val userId = JwtUtils.getUserIdFromToken(token)
                        val remoteDS = repository.getRemoteDataSource()

                        // Ejecutar peticiones independientes en paralelo para evitar bloqueo
                        val pacientesResult: Result<List<PacienteDto>>
                        val atencionesResult: Result<List<AtencionCronicoDto>>
                        val pacientePatologiaList: List<PacientePatologiaDto>
                        val medicosCatalog: List<MedicoDto>
                        val allSolicitudesList: List<SolicitudExamenDto>
                        val resultadosList: List<ResultadoExamenDto>
                        val examenesCatalog: List<ExamenLaboratorioDto>
                        val directExamsAllList: List<ExamDto>
                        val medicamentosCatalog: List<MedicamentoCatalogDto>
                        val directMedsList: List<MedicamentoAtencionDto>
                        val tratamientosList: List<TratamientoDto>
                        val detallesList: List<DetalleTratamientoDto>
                        val resumenResult: com.sistema.paciencare.data.remote.dto.ResumenMovilDto?

                        coroutineScope {
                            val defPacientes = async(Dispatchers.IO) { remoteDS.getPacienteDelUsuario(userId ?: 0, token) }
                            val defAtenciones = async(Dispatchers.IO) { remoteDS.getAllAtencionesCronicasExact(token) }
                            val defPatologias = async(Dispatchers.IO) { remoteDS.getPacientePatologiaExact(token) }
                            val defMedicos = async(Dispatchers.IO) { remoteDS.getMedicosExact(token) }
                            val defSolicitudes = async(Dispatchers.IO) { remoteDS.getSolicitudesDeExamenesExact(token) }
                            val defResultados = async(Dispatchers.IO) { remoteDS.getResultadosExamenesExact(token) }
                            val defExamenesCatalog = async(Dispatchers.IO) { remoteDS.getExamenesLaboratorioCatalogExact(token) }
                            val defDirectExamsAll = async(Dispatchers.IO) { remoteDS.getExamenes(token, null) }
                            val defMedsCatalog = async(Dispatchers.IO) { remoteDS.getMedicamentosCatalogExact(token) }
                            val defDirectMeds = async(Dispatchers.IO) { remoteDS.getMedicamentos(token, null) }
                            val defTratamientos = async(Dispatchers.IO) { remoteDS.getTratamientosExact(token) }
                            val defDetalles = async(Dispatchers.IO) { remoteDS.getDetallesTratamientoExact(token) }
                            val defResumen = async(Dispatchers.IO) { remoteDS.getResumenMovil(token).getOrNull() }

                            pacientesResult = defPacientes.await()
                            atencionesResult = defAtenciones.await()
                            pacientePatologiaList = defPatologias.await().getOrNull() ?: emptyList()
                            medicosCatalog = defMedicos.await().getOrNull() ?: emptyList()
                            allSolicitudesList = defSolicitudes.await().getOrNull() ?: emptyList()
                            resultadosList = defResultados.await().getOrNull() ?: emptyList()
                            examenesCatalog = defExamenesCatalog.await().getOrNull() ?: emptyList()
                            directExamsAllList = defDirectExamsAll.await().getOrNull() ?: emptyList()
                            medicamentosCatalog = defMedsCatalog.await().getOrNull() ?: emptyList()
                            directMedsList = defDirectMeds.await().getOrNull() ?: emptyList()
                            tratamientosList = defTratamientos.await().getOrNull() ?: emptyList()
                            detallesList = defDetalles.await().getOrNull() ?: emptyList()
                            resumenResult = defResumen.await()
                        }

                        val pacientesList = pacientesResult.getOrNull()
                        val allAtenciones = atencionesResult.getOrDefault(emptyList())

                        if (pacientesResult.isFailure && pacientesList == null) {
                            return@withTimeoutOrNull
                        }

                        val safePacientesList = pacientesList ?: emptyList()
                        val userCedula = userObj?.cedula?.trim()?.lowercase() ?: ""

                        val emailPrefix = activeEmail.substringBefore("@").lowercase()

                        val paciente = safePacientesList.find { userCedula.length > 3 && !it.cedula.isNullOrBlank() && userCedula.equals(it.cedula.trim(), ignoreCase = true) }
                            ?: safePacientesList.find { !it.email.isNullOrBlank() && activeEmail.equals(it.email.trim(), ignoreCase = true) }
                            ?: safePacientesList.find { activeEmail.equals(it.usuarioEmail?.trim(), ignoreCase = true) }
                            ?: safePacientesList.find { userId != null && userId > 0 && it.usuario == userId }
                            ?: safePacientesList.find {
                                val pNombre = "${it.nombre ?: ""} ${it.apellidos ?: ""}".trim().lowercase()
                                pNombre.isNotBlank() && emailPrefix.isNotBlank() && (
                                    pNombre.contains(emailPrefix) ||
                                    emailPrefix.contains(pNombre.take(4)) ||
                                    (pNombre.split(" ").firstOrNull()?.let { fName -> fName.length >= 3 && emailPrefix.contains(fName) } == true)
                                )
                            }
                            ?: if (safePacientesList.size == 1) safePacientesList.first() else null

                        val pacienteId = paciente?.id

                        val deletedApptIds = repository.getSessionManager().getDeletedAppointmentIds(activeEmail)
                        val deletedExamIds = repository.getSessionManager().getDeletedExamIds(activeEmail)
                        val deletedMedIds = repository.getSessionManager().getDeletedMedicationIds(activeEmail)

                        if (paciente != null && userObj != null) {
                            val fn = paciente.nombre?.trim() ?: ""
                            val ap = paciente.apellidos?.trim() ?: ""
                            val fullNameFromApi = when {
                                fn.isNotEmpty() && ap.isNotEmpty() -> "$fn $ap"
                                fn.isNotEmpty() -> fn
                                ap.isNotEmpty() -> ap
                                else -> ""
                            }

                            val genderFormatted = when {
                                paciente.sexo?.startsWith("F", ignoreCase = true) == true -> "Femenino"
                                paciente.sexo?.startsWith("M", ignoreCase = true) == true -> "Masculino"
                                else -> paciente.sexo?.ifBlank { null } ?: userObj.gender
                            }

                            val bestName = if (fullNameFromApi.isNotBlank()) fullNameFromApi else userObj.name
                            val bestCedula = paciente.cedula?.ifBlank { null } ?: userObj.cedula
                            val bestBirthDate = paciente.fechaDeNacimiento?.ifBlank { null } ?: userObj.birthDate
                            val bestGender = genderFormatted
                            val bestPhone = paciente.celular?.ifBlank { null } ?: userObj.phone
                            val bestAddress = paciente.direccion?.ifBlank { null } ?: userObj.address

                            val updatedUser = userObj.copy(
                                name = bestName,
                                cedula = bestCedula,
                                birthDate = bestBirthDate,
                                gender = bestGender,
                                phone = bestPhone,
                                address = bestAddress,
                                isFemale = bestGender.equals("Femenino", ignoreCase = true)
                            )
                            repository.updateUser(updatedUser)
                            repository.getSessionManager().saveSession(true, bestName, updatedUser.isFemale, updatedUser.role, token, null, activeEmail)
                        }

                        val pacientePatologiaIds = (pacientePatologiaList
                            .filter { pacienteId != null && it.pacienteId == pacienteId }
                            .mapNotNull { it.id } + setOfNotNull(pacienteId)).toSet()

                        val atencionesDirect = repository.getRemoteDataSource().getAtencionesCronicas(token, pacienteId, userId).getOrNull() ?: emptyList()

                        val atencionesDelPaciente = if (atencionesDirect.isNotEmpty()) {
                            atencionesDirect
                        } else {
                            allAtenciones.filter { at ->
                                at.matchesPacienteOrUser(pacienteId, userId) ||
                                (at.idPacientePatologia != null && at.idPacientePatologia in pacientePatologiaIds) ||
                                (at.pacienteIdDeep != null && (at.pacienteIdDeep == pacienteId || at.pacienteIdDeep == userId))
                            }
                        }

                        val atencionesIds = atencionesDelPaciente.map { it.id }.toSet()

                        val apptsToInsert = mutableListOf<AppointmentEntity>()
                        val defaultDoctor = medicosCatalog.firstOrNull()?.getFullName() ?: ""

                        val sdfIsoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val sdfSlashDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val todayDate = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time

                        atencionesDelPaciente.forEach { atencion ->
                            val nextDateStr = atencion.fechaProximaCita?.ifBlank { null }

                            val medicoObj = atencion.idMedicoInt?.let { mId -> medicosCatalog.find { it.id == mId } }
                            val doctorVal = atencion.doctorName
                                ?: medicoObj?.getFullName()
                                ?: defaultDoctor

                            if (!nextDateStr.isNullOrBlank() && atencion.id.toString() !in deletedApptIds) {
                                val formattedNextDate = nextDateStr.take(10)
                                val rawTimeVal = atencion.hora?.ifBlank { null }
                                val formattedNextTime = when {
                                    !rawTimeVal.isNullOrBlank() -> parseIsoTime("T$rawTimeVal")
                                    nextDateStr.length > 10 -> parseIsoTime(nextDateStr)
                                    else -> "08:00 AM"
                                }

                                val parsedNextDate = try {
                                    if (formattedNextDate.contains("-")) sdfIsoDate.parse(formattedNextDate)
                                    else sdfSlashDate.parse(formattedNextDate)
                                } catch (_: Exception) { null }

                                val isPastNext = parsedNextDate != null && parsedNextDate.before(todayDate)

                                val statusNext = when {
                                    isPastNext -> "Realizada"
                                    atencion.asistio?.startsWith("C", ignoreCase = true) == true -> "Cancelada"
                                    else -> "Confirmada"
                                }

                                apptsToInsert.add(
                                    AppointmentEntity(
                                        id = atencion.id,
                                        userEmail = activeEmail,
                                        date = formattedNextDate,
                                        time = formattedNextTime,
                                        doctor = doctorVal,
                                        reason = atencion.observaciones?.ifBlank { null } ?: "Control y Próxima Cita",
                                        status = statusNext
                                    )
                                )
                            }
                        }

                        resumenResult?.proximaCita?.let { prox ->
                            val proxDate = prox.fecha?.take(10)
                            if (!proxDate.isNullOrBlank()) {
                                apptsToInsert.add(
                                    AppointmentEntity(
                                        id = prox.id ?: 9999,
                                        userEmail = activeEmail,
                                        date = proxDate,
                                        time = parseIsoTime(prox.hora ?: prox.fecha),
                                        doctor = prox.medico?.ifBlank { null } ?: defaultDoctor,
                                        reason = prox.patologia?.ifBlank { null } ?: "Control y Próxima Cita",
                                        status = "Confirmada"
                                    )
                                )
                            }
                        }

                        repository.clearAppointmentsForUser(activeEmail)
                        if (apptsToInsert.isNotEmpty()) {
                            apptsToInsert.forEach { repository.insertAppointment(it) }
                        }

                        // 2. Sincronizar Exámenes
                        val examsToInsert = mutableListOf<ExamEntity>()

                        val safeSolicitudes = if (atencionesIds.isNotEmpty()) {
                            allSolicitudesList.filter { 
                                it.idAtencionCronico != null && it.idAtencionCronico in atencionesIds 
                            }
                        } else emptyList()

                        val solicitudIds = safeSolicitudes.mapNotNull { it.id }.toSet()

                        if (safeSolicitudes.isNotEmpty()) {
                            val resultadosDelPaciente = resultadosList.filter { it.idSolicitudExamen in solicitudIds }

                            val combinedExams = if (resultadosDelPaciente.isNotEmpty()) {
                                resultadosDelPaciente.mapIndexed { idx, res ->
                                    val sol = safeSolicitudes.find { it.id == res.idSolicitudExamen }
                                    Triple(res.id ?: (idx + 1), sol, res)
                                }
                            } else {
                                safeSolicitudes.mapIndexed { idx, sol ->
                                    Triple(sol.id ?: (idx + 1), sol, null)
                                }
                            }

                            combinedExams.forEach { (examId, solicitud, resultado) ->
                                if (examId.toString() !in deletedExamIds) {
                                    val catalogExam = examenesCatalog.find { it.id == solicitud?.idExamenDeLaboratorio }
                                    val examName = solicitud?.nombreExamenDirect?.ifBlank { null }
                                        ?: catalogExam?.nombreExamen?.ifBlank { null } 
                                        ?: when (Math.abs(examId) % 4) {
                                            0 -> "Biometría Hemática Completa"
                                            1 -> "Hemoglobina Glicosilada (HbA1c)"
                                            2 -> "Glucosa en Sangre y Perfil Lipídico"
                                            else -> "Examen General de Orina (EGO)"
                                        }

                                    val indicationsStr = solicitud?.indicaciones?.ifBlank { null } ?: ""

                                    val rawDateStr = resultado?.fechaResultado ?: solicitud?.fechaDeEnvio
                                    val dateOnly = rawDateStr?.take(10) ?: ""
                                    val timeOnly = parseIsoTime(rawDateStr)
                                    val statusStr = resultado?.interpretacion?.ifBlank { null } ?: "Pendiente"

                                    examsToInsert.add(
                                        ExamEntity(
                                            id = examId,
                                            userEmail = activeEmail,
                                            name = examName,
                                            doctor = defaultDoctor,
                                            date = dateOnly,
                                            time = timeOnly,
                                            indications = indicationsStr,
                                            status = statusStr,
                                            statusColor = if (statusStr.equals("Pendiente", ignoreCase = true)) 
                                                Color.parseColor("#F59E0B") 
                                            else 
                                                Color.parseColor("#10B981")
                                        )
                                    )
                                }
                            }
                        }

                        val directExamsList = kotlinx.coroutines.withContext(Dispatchers.IO) {
                            remoteDS.getExamenes(token, pacienteId).getOrNull() ?: emptyList()
                        }
                        val combinedDirectExams = (directExamsList + directExamsAllList.filter { pacienteId != null && (it.pacienteId == pacienteId || it.pacienteId == userId) }).distinctBy { it.id ?: it.nombre }
                        combinedDirectExams.forEachIndexed { index, directExam ->
                            val eId = directExam.id ?: (index + 40000)
                            if (eId.toString() !in deletedExamIds) {
                                val catalogExam = examenesCatalog.find { it.id == directExam.idExamen }
                                val eName = directExam.nombre?.ifBlank { null }
                                    ?: catalogExam?.nombreExamen?.ifBlank { null }
                                    ?: when (Math.abs(eId) % 4) {
                                        0 -> "Biometría Hemática Completa"
                                        1 -> "Hemoglobina Glicosilada (HbA1c)"
                                        2 -> "Glucosa en Sangre y Perfil Lipídico"
                                        else -> "Examen General de Orina (EGO)"
                                    }
                                val statusStr = directExam.interpretacion?.ifBlank { null } ?: "Completado"
                                val dateOnly = directExam.fecha?.take(10) ?: ""
                                val doctorStr = directExam.doctorName?.ifBlank { null } ?: defaultDoctor

                                if (examsToInsert.none { it.name.equals(eName, ignoreCase = true) && it.date == dateOnly }) {
                                    examsToInsert.add(
                                        ExamEntity(
                                            id = eId,
                                            userEmail = activeEmail,
                                            name = eName,
                                            doctor = doctorStr,
                                            date = dateOnly,
                                            time = parseIsoTime(directExam.fecha),
                                            indications = "Laboratorio Clínico",
                                            status = statusStr,
                                            statusColor = if (statusStr.equals("Pendiente", ignoreCase = true)) 
                                                Color.parseColor("#F59E0B") 
                                            else 
                                                Color.parseColor("#10B981")
                                        )
                                    )
                                }
                            }
                        }

                        resumenResult?.examenesSolicitados?.forEachIndexed { idx, ex ->
                            val exName = ex.nombre?.ifBlank { null }
                            if (!exName.isNullOrBlank() && examsToInsert.none { it.name.equals(exName, ignoreCase = true) }) {
                                val dateStr = ex.fecha?.take(10) ?: ""
                                val statusStr = ex.resultadoText?.ifBlank { null } ?: "Pendiente"
                                examsToInsert.add(
                                    ExamEntity(
                                        id = ex.id ?: (idx + 30000),
                                        userEmail = activeEmail,
                                        name = exName,
                                        doctor = defaultDoctor,
                                        date = dateStr,
                                        time = parseIsoTime(ex.fecha),
                                        indications = ex.indicaciones?.ifBlank { null } ?: "Laboratorio Clínico",
                                        status = statusStr,
                                        statusColor = if (statusStr.equals("Pendiente", ignoreCase = true)) 
                                            Color.parseColor("#F59E0B") 
                                        else 
                                            Color.parseColor("#10B981")
                                    )
                                )
                            }
                        }

                        repository.clearExamsForUser(activeEmail)
                        if (examsToInsert.isNotEmpty()) {
                            examsToInsert.forEach { repository.insertExam(it) }
                        }

                        // 3. Sincronizar Medicamentos
                        val medsToInsert = mutableListOf<MedicationEntity>()

                        val tratamientosDelPaciente = tratamientosList.filter { 
                            it.idAtencionCronico != null && atencionesIds.isNotEmpty() && it.idAtencionCronico in atencionesIds
                        }
                        val tratamientoIds = tratamientosDelPaciente.mapNotNull { it.id }.toSet()

                        val detallesDelPaciente = detallesList.filter { detalle ->
                            (detalle.idTratamiento != null && detalle.idTratamiento in tratamientoIds) ||
                            (pacienteId != null && detalle.pacienteId == pacienteId)
                        }

                        detallesDelPaciente.forEachIndexed { index, detalle ->
                            val medIdInt = detalle.id ?: (index + 1)
                            val medIdStr = medIdInt.toString()
                            if (medIdStr !in deletedMedIds) {
                                val catalogMed = medicamentosCatalog.find { it.id == detalle.idMedicamento }
                                val directMed = directMedsList.find { it.id == detalle.idMedicamento || it.id == detalle.id }

                                val medName = catalogMed?.nombreMedicamento
                                    ?: directMed?.nombre
                                    ?: detalle.nombreMedicamento
                                    ?: "Medicamento"
                                
                                val conc = catalogMed?.concentracion?.ifBlank { null } ?: directMed?.presentacion?.ifBlank { null }
                                val pres = catalogMed?.presentacion?.ifBlank { null } ?: directMed?.forma?.ifBlank { null }
                                val doseStr = if (!conc.isNullOrBlank() && !pres.isNullOrBlank()) {
                                    "$conc — $pres"
                                } else conc ?: pres ?: "Dosis Indicada"

                                val freqStr = detalle.frecuencia?.ifBlank { null } ?: directMed?.forma?.ifBlank { null } ?: "1 vez por día"

                                medsToInsert.add(
                                    MedicationEntity(
                                        id = medIdInt,
                                        userEmail = activeEmail,
                                        name = medName,
                                        dose = doseStr,
                                        frequency = freqStr,
                                        duration = "Tratamiento Activo",
                                        doctor = defaultDoctor,
                                        status = "Activo"
                                    )
                                )
                            }
                        }

                        val filteredDirectMeds = directMedsList.filter { pacienteId != null && (it.pacienteId == pacienteId || it.pacienteId == userId) }
                        filteredDirectMeds.forEachIndexed { index, directMed ->
                            val mIdInt = directMed.id ?: (index + 20000)
                            val mIdStr = mIdInt.toString()
                            val mName = directMed.nombre
                            if (!mName.isNullOrBlank() && mIdStr !in deletedMedIds && medsToInsert.none { it.name.equals(mName, ignoreCase = true) }) {
                                val doseStr = directMed.presentacion?.ifBlank { null } ?: "Dosis Indicada"
                                val freqStr = directMed.forma?.ifBlank { null } ?: "Frecuencia no especificada"
                                medsToInsert.add(
                                    MedicationEntity(
                                        id = mIdInt,
                                        userEmail = activeEmail,
                                        name = mName,
                                        dose = doseStr,
                                        frequency = freqStr,
                                        duration = "Tratamiento Activo",
                                        doctor = defaultDoctor,
                                        status = "Activo"
                                    )
                                )
                            }
                        }

                        resumenResult?.medicamentos?.forEachIndexed { idx, med ->
                            val medName = med.nombre?.ifBlank { null }
                            if (!medName.isNullOrBlank() && medsToInsert.none { it.name.equals(medName, ignoreCase = true) }) {
                                val conc = med.concentracion?.ifBlank { null }
                                val pres = med.presentacion?.ifBlank { null }
                                val doseStr = if (!conc.isNullOrBlank() && !pres.isNullOrBlank()) "$conc — $pres" else conc ?: pres ?: "Dosis Indicada"
                                val freqStr = med.indicacion?.ifBlank { null } ?: "1 vez al día"
                                medsToInsert.add(
                                    MedicationEntity(
                                        id = med.id ?: (idx + 30000),
                                        userEmail = activeEmail,
                                        name = medName,
                                        dose = doseStr,
                                        frequency = freqStr,
                                        duration = "Tratamiento Activo",
                                        doctor = defaultDoctor,
                                        status = med.estado?.ifBlank { null } ?: "Activo"
                                    )
                                )
                            }
                        }

                        if (medsToInsert.isNotEmpty()) {
                            repository.clearMedicationsForUser(activeEmail)
                            medsToInsert.forEach { repository.insertMedication(it) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PACI_DEBUG", "Error syncRemoteData: ${e.message}", e)
                } finally {
                    isSyncing = false
                }
            }
        }
    }

    private fun parseIsoTime(isoString: String?): String {
        if (isoString.isNullOrBlank() || !isoString.contains("T")) return "08:00 AM"
        val rawTime = isoString.substringAfter("T").take(5)
        val parts = rawTime.split(":")
        if (parts.size >= 2) {
            val hour = parts[0].toIntOrNull() ?: 8
            val min = parts[1]
            val amPm = if (hour >= 12) "PM" else "AM"
            val formattedHour = if (hour % 12 == 0) 12 else hour % 12
            val padMin = if (min.length == 1) "0$min" else min
            return String.format(Locale.US, "%02d:%s %s", formattedHour, padMin, amPm)
        }
        return "08:00 AM"
    }

    fun updateUser(user: UserEntity) = viewModelScope.launch {
        repository.updateUser(user)

        val token = repository.getSessionManager().getAccessToken()
        val activeEmail = _currentUserEmail.value
        if (!token.isNullOrBlank() && activeEmail.isNotBlank()) {
            val userId = JwtUtils.getUserIdFromToken(token)
            val pacientesList = repository.getRemoteDataSource().getPacienteDelUsuario(userId ?: 0, token).getOrNull() ?: emptyList()
            val paciente = pacientesList.find { userId != null && userId > 0 && it.usuario == userId }
                ?: pacientesList.find { !it.email.isNullOrBlank() && activeEmail.equals(it.email.trim(), ignoreCase = true) }
                ?: pacientesList.find { activeEmail.equals(it.usuarioEmail?.trim(), ignoreCase = true) }

            val nameParts = user.name.trim().split(Regex("\\s+"))
            val pNombre = nameParts.firstOrNull() ?: user.name
            val pApellidos = nameParts.drop(1).joinToString(" ")

            val userElem = if (userId != null && userId > 0) com.google.gson.JsonPrimitive(userId) else null
            val pId = paciente?.id
            if (pId != null && pId > 0) {
                val dto = PacienteDto(
                    id = pId,
                    usuarioElement = userElem,
                    nombre = pNombre,
                    apellidos = pApellidos,
                    sexo = user.gender,
                    fechaDeNacimiento = user.birthDate,
                    celular = user.phone,
                    cedula = user.cedula,
                    direccion = user.address,
                    barrio = null,
                    email = user.email,
                    estado = true
                )
                repository.getRemoteDataSource().updatePacienteRemote(token, pId, dto)
            } else {
                val dto = PacienteDto(
                    id = 0,
                    usuarioElement = userElem,
                    nombre = pNombre,
                    apellidos = pApellidos,
                    sexo = user.gender,
                    fechaDeNacimiento = user.birthDate,
                    celular = user.phone,
                    cedula = user.cedula,
                    direccion = user.address,
                    barrio = null,
                    email = user.email,
                    estado = true
                )
                val createResult = repository.getRemoteDataSource().createPacienteRemote(token, dto)
                createResult.onSuccess {
                    syncRemoteData()
                }
            }
        }
    }

    // --- CITAS ---
    val allAppointments: StateFlow<List<AppointmentEntity>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email.isEmpty()) flowOf(emptyList())
            else repository.getAppointmentsForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertAppointment(appointment: AppointmentEntity) = viewModelScope.launch {
        val email = _currentUserEmail.value
        val entity = if (appointment.userEmail.isEmpty() && email.isNotEmpty()) appointment.copy(userEmail = email) else appointment
        repository.insertAppointment(entity)
    }

    fun deleteAppointment(appointment: AppointmentEntity) = viewModelScope.launch {
        val email = _currentUserEmail.value.ifBlank { appointment.userEmail }
        if (email.isNotBlank()) {
            repository.getSessionManager().addDeletedAppointmentId(email, appointment.id.toString())
        }
        repository.deleteAppointment(appointment)
    }

    // --- MEDICAMENTOS ---
    val activeMedications: StateFlow<List<MedicationEntity>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email.isEmpty()) flowOf(emptyList())
            else repository.getActiveMedicationsForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedications: StateFlow<List<MedicationEntity>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email.isEmpty()) flowOf(emptyList())
            else repository.getMedicationsForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertMedication(medication: MedicationEntity) = viewModelScope.launch {
        val email = _currentUserEmail.value
        val entity = if (medication.userEmail.isEmpty() && email.isNotEmpty()) medication.copy(userEmail = email) else medication
        repository.insertMedication(entity)
    }

    fun deleteMedication(medication: MedicationEntity) = viewModelScope.launch {
        val email = _currentUserEmail.value.ifBlank { medication.userEmail }
        if (email.isNotBlank()) {
            repository.getSessionManager().addDeletedMedicationId(email, medication.id.toString())
        }
        repository.deleteMedication(medication)
    }

    // --- EXÁMENES ---
    val allExams: StateFlow<List<ExamEntity>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email.isEmpty()) flowOf(emptyList())
            else repository.getExamsForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertExam(exam: ExamEntity) = viewModelScope.launch {
        val email = _currentUserEmail.value
        val entity = if (exam.userEmail.isEmpty() && email.isNotEmpty()) exam.copy(userEmail = email) else exam
        repository.insertExam(entity)
    }

    fun deleteExam(exam: ExamEntity) = viewModelScope.launch {
        val email = _currentUserEmail.value.ifBlank { exam.userEmail }
        if (email.isNotBlank()) {
            repository.getSessionManager().addDeletedExamId(email, exam.id.toString())
        }
        repository.deleteExam(exam)
    }

    // --- EVALUACIONES ---
    val allEvaluations: StateFlow<List<EvaluationEntity>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email.isEmpty()) flowOf(emptyList())
            else repository.getEvaluationsForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertEvaluation(evaluation: EvaluationEntity) = viewModelScope.launch {
        val email = _currentUserEmail.value
        val entity = if (evaluation.userEmail.isEmpty() && email.isNotEmpty()) evaluation.copy(userEmail = email) else evaluation
        repository.insertEvaluation(entity)
    }

    fun deleteEvaluation(evaluation: EvaluationEntity) = viewModelScope.launch {
        val email = _currentUserEmail.value.ifBlank { evaluation.userEmail }
        if (email.isNotBlank()) {
            repository.getSessionManager().addDeletedEvaluationId(email, evaluation.id.toString())
        }
        repository.deleteEvaluation(evaluation)
    }

    fun clearAllEvaluations() = viewModelScope.launch {
        repository.clearAllEvaluations()
    }

    fun clearAllAppointments() = viewModelScope.launch {
        repository.clearAllAppointments()
    }

    private val _managerAppointments = MutableStateFlow<List<AppointmentEntity>>(emptyList())
    val globalAllAppointments: StateFlow<List<AppointmentEntity>> = _managerAppointments.asStateFlow()

    private val _managerMedications = MutableStateFlow<List<MedicationEntity>>(emptyList())
    val globalAllMedications: StateFlow<List<MedicationEntity>> = _managerMedications.asStateFlow()

    private val _managerTreatmentsCount = MutableStateFlow(0)
    val managerTreatmentsCount: StateFlow<Int> = _managerTreatmentsCount.asStateFlow()

    private val _managerExams = MutableStateFlow<List<ExamEntity>>(emptyList())
    val globalAllExams: StateFlow<List<ExamEntity>> = _managerExams.asStateFlow()

    private val _managerPatientsCount = MutableStateFlow(0)
    val managerPatientsCount: StateFlow<Int> = _managerPatientsCount.asStateFlow()

    private val _managerEvaluations = MutableStateFlow<List<EvaluationEntity>>(emptyList())
    val globalAllEvaluations: StateFlow<List<EvaluationEntity>> = repository.allEvaluations
        .combine(_managerEvaluations) { local, remote -> (remote + local).distinctBy { "${it.userEmail}_${it.date}_${it.overallRating}_${it.comments}" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun syncManagerData() {
        val token = repository.getSessionManager().getAccessToken()
        if (!token.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    val remoteDS = repository.getRemoteDataSource()

                    val medicosCatalog: List<MedicoDto>
                    val allPatientsList: List<PacienteDto>
                    val atencionesResult: Result<List<AtencionCronicoDto>>
                    val catalogMeds: List<MedicamentoCatalogDto>
                    val directMeds: List<MedicamentoAtencionDto>
                    val tratamientosResult: Result<List<TratamientoDto>>
                    val resultadosResult: Result<List<ResultadoExamenDto>>
                    val catalogExams: List<ExamenLaboratorioDto>

                    coroutineScope {
                        val defMedicos = async(Dispatchers.IO) { remoteDS.getMedicosExact(token) }
                        val defPacientes = async(Dispatchers.IO) { remoteDS.getPacienteDelUsuario(0, token) }
                        val defAtenciones = async(Dispatchers.IO) { remoteDS.getAllAtencionesCronicasExact(token) }
                        val defCatalogMeds = async(Dispatchers.IO) { remoteDS.getMedicamentosCatalogExact(token) }
                        val defDirectMeds = async(Dispatchers.IO) { remoteDS.getMedicamentos(token, 1) }
                        val defTratamientos = async(Dispatchers.IO) { remoteDS.getTratamientosExact(token) }
                        val defResultados = async(Dispatchers.IO) { remoteDS.getResultadosExamenesExact(token) }
                        val defCatalogExams = async(Dispatchers.IO) { remoteDS.getExamenesLaboratorioCatalogExact(token) }

                        medicosCatalog = defMedicos.await().getOrNull() ?: emptyList()
                        allPatientsList = defPacientes.await().getOrNull() ?: emptyList()
                        atencionesResult = defAtenciones.await()
                        catalogMeds = defCatalogMeds.await().getOrNull() ?: emptyList()
                        directMeds = defDirectMeds.await().getOrNull() ?: emptyList()
                        tratamientosResult = defTratamientos.await()
                        resultadosResult = defResultados.await()
                        catalogExams = defCatalogExams.await().getOrNull() ?: emptyList()
                    }

                    val defaultDoctorName = medicosCatalog.firstOrNull()?.getFullName() ?: ""
                    _managerPatientsCount.value = allPatientsList.size

                    // 2. Descargar Citas globales
                    atencionesResult.onSuccess { atencionesList ->
                        val appts = atencionesList.mapNotNull { atencion ->
                            val dateStr = atencion.fechaProximaCita?.ifBlank { null }
                                ?: atencion.fechaAtencion?.ifBlank { null }
                            if (!dateStr.isNullOrBlank()) {
                                val medicoObj = atencion.idMedicoInt?.let { mId -> medicosCatalog.find { it.id == mId } }
                                val docName = atencion.doctorName ?: medicoObj?.getFullName() ?: defaultDoctorName

                                val statusStr = when {
                                    atencion.asistio?.startsWith("S", ignoreCase = true) == true || atencion.asistio?.startsWith("Y", ignoreCase = true) == true || atencion.asistio?.startsWith("T", ignoreCase = true) == true || atencion.asistio?.equals("1") == true -> "Realizada"
                                    atencion.asistio?.startsWith("C", ignoreCase = true) == true -> "Cancelada"
                                    else -> "Pendiente"
                                }

                                AppointmentEntity(
                                    id = atencion.id,
                                    userEmail = "clinica_global",
                                    date = dateStr,
                                    time = "08:00 AM",
                                    doctor = docName,
                                    reason = atencion.observaciones?.ifBlank { null } ?: "Consulta Médica",
                                    status = statusStr
                                )
                            } else null
                        }
                        _managerAppointments.value = appts.distinctBy { it.id }
                    }

                    // 3. Descargar Fármacos
                    val cleanMedsList = if (catalogMeds.isNotEmpty()) {
                        catalogMeds.mapIndexed { idx, m ->
                            MedicationEntity(
                                id = m.id ?: (idx + 1),
                                userEmail = "clinica_global",
                                name = m.nombreMedicamento ?: "Medicamento",
                                dose = m.concentracion ?: "Dosis Indicada",
                                frequency = m.presentacion ?: "1 vez al día",
                                duration = "Disponible",
                                doctor = defaultDoctorName,
                                status = "Activo"
                            )
                        }
                    } else {
                        directMeds.mapIndexed { idx, m ->
                            MedicationEntity(
                                id = m.id ?: (idx + 1),
                                userEmail = "clinica_global",
                                name = m.nombre ?: "Medicamento",
                                dose = m.presentacion ?: "Dosis Indicada",
                                frequency = m.forma ?: "1 vez al día",
                                duration = "Disponible",
                                doctor = defaultDoctorName,
                                status = "Activo"
                            )
                        }
                    }
                    _managerMedications.value = cleanMedsList.distinctBy { it.name }

                    // 3B. Descargar Tratamientos
                    tratamientosResult.onSuccess { tratamientosList ->
                        _managerTreatmentsCount.value = tratamientosList.size
                    }

                    // 4. Descargar Exámenes globales
                    resultadosResult.onSuccess { resultadosList ->
                        val exams = resultadosList.mapIndexed { index, resultado ->
                            val catalogExam = catalogExams.find { it.id == resultado.idSolicitudExamen }
                            val examName = catalogExam?.nombreExamen ?: "Examen de Laboratorio"
                            ExamEntity(
                                id = resultado.id ?: (index + 1),
                                userEmail = "clinica_global",
                                name = examName,
                                doctor = "Laboratorio Clínico",
                                date = resultado.fechaResultado?.take(10) ?: "",
                                time = "08:00 AM",
                                indications = "",
                                status = resultado.interpretacion ?: "Completado",
                                statusColor = Color.parseColor("#10B981")
                            )
                        }
                        _managerExams.value = exams.distinctBy { it.id }
                    }
                } catch (e: Exception) {
                    Log.e("PACI_DEBUG", "Error syncManagerData: ${e.message}", e)
                }
            }
        }
    }
}

class PatientViewModelFactory(private val repository: PacienCareRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatientViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatientViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
