package com.sistema.paciencare

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.sistema.paciencare.data.local.SessionManager
import com.sistema.paciencare.model.ScreenState
import com.sistema.paciencare.ui.auth.ForgotPasswordScreen
import com.sistema.paciencare.ui.auth.LoginScreen
import com.sistema.paciencare.ui.auth.RegisterScreen
import com.sistema.paciencare.ui.patient.DashboardScreen
import com.sistema.paciencare.ui.patient.EvaluationScreen
import com.sistema.paciencare.ui.patient.MyAppointmentsScreen
import com.sistema.paciencare.ui.patient.MedicationsScreen
import com.sistema.paciencare.ui.patient.ProfileScreen
import com.sistema.paciencare.ui.patient.ExamsScreen
import com.sistema.paciencare.ui.manager.ManagerDashboardScreen
import com.sistema.paciencare.viewmodel.*
import androidx.lifecycle.viewmodel.compose.viewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val app = (context.applicationContext as PacienCareApplication)
                val repository = app.repository

                val sessionManager = remember(context) {
                    SessionManager(context)
                }

                val patientViewModel: PatientViewModel = viewModel(
                    factory = PatientViewModelFactory(repository)
                )

                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(
                        repository = repository,
                        sessionManager = sessionManager
                    )
                )

                MainAppContent(
                    patientViewModel,
                    authViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppContent(patientViewModel: PatientViewModel, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var currentScreen by remember { mutableStateOf(ScreenState.Login) }
    var selectedRole by remember { mutableStateOf("Paciente") }
    var loginEmail by remember { mutableStateOf("") }
    var backPressedTime by remember { mutableLongStateOf(0L) }

    // Interceptar botón / gesto físico de regreso del sistema Android
    BackHandler {
        when (currentScreen) {
            ScreenState.Register, ScreenState.ForgotPassword -> {
                currentScreen = ScreenState.Login
            }
            ScreenState.Evaluation, ScreenState.MyAppointments, ScreenState.MyMedications,
            ScreenState.MyProfile, ScreenState.MyExams -> {
                currentScreen = ScreenState.PatientDashboard
            }
            ScreenState.PatientDashboard, ScreenState.ManagerDashboard, ScreenState.Login -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < 2000) {
                    (context as? Activity)?.finish()
                } else {
                    backPressedTime = currentTime
                    Toast.makeText(context, "Presiona de nuevo para salir de PacienCare", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Efecto para observar red y cargar datos de usuario si ya está logueado
    LaunchedEffect(Unit) {
        patientViewModel.observeNetwork(context)
        if (sessionManager.isLoggedIn()) {
            val savedEmail = sessionManager.getUserEmail()?.ifBlank { null }
                ?: loginEmail.ifBlank { null }
                ?: ""
            if (savedEmail.isNotBlank()) {
                patientViewModel.loadUser(savedEmail)
            }
        }
    }

    // Protector de Navegación por Roles (Role Guard)
    LaunchedEffect(currentScreen) {
        if (sessionManager.isLoggedIn()) {
            val realUserRole = sessionManager.getUserRole() ?: "Paciente"
            if (realUserRole.equals("Paciente", ignoreCase = true) && currentScreen == ScreenState.ManagerDashboard) {
                currentScreen = ScreenState.PatientDashboard
                Toast.makeText(context, "Acceso restringido: Solo el Gerente puede ingresar al Panel Administrativo", Toast.LENGTH_SHORT).show()
            } else if (realUserRole.equals("Gerencia", ignoreCase = true) && currentScreen != ScreenState.ManagerDashboard && currentScreen != ScreenState.Login) {
                currentScreen = ScreenState.ManagerDashboard
            }
        }
    }

    // Observar resultado de login
    val loginSuccess by authViewModel.loginResult.collectAsState()
    val loggedUser by authViewModel.loggedUser.collectAsState()

    LaunchedEffect(loginSuccess) {
        if (loginSuccess == true) {

            val rolReal = sessionManager.getUserRole() ?: selectedRole

            patientViewModel.loadUser(loginEmail)

            currentScreen = if (rolReal.equals("Gerencia", ignoreCase = true)) {
                ScreenState.ManagerDashboard
            } else {
                ScreenState.PatientDashboard
            }

            authViewModel.resetLoginResult()
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = true
        }
    }

    when (currentScreen) {
        ScreenState.Login -> {
            // Reset state when entering the login screen
            LaunchedEffect(Unit) {
                authViewModel.resetLoginResult()
            }
            val errorType by authViewModel.loginErrorType.collectAsState()
            val isLoading by authViewModel.isLoading.collectAsState()
            LoginScreen(
                selectedRole = selectedRole,
                onRoleChange = { selectedRole = it },
                onNavigateToRegister = { currentScreen = ScreenState.Register },
                onNavigateToForgot = { currentScreen = ScreenState.ForgotPassword },
                isError = loginSuccess == false,
                errorType = errorType,
                isLoading = isLoading,
                onLoginClick = { email, password ->
                    loginEmail = email
                    authViewModel.loginUser(email, password, selectedRole)
                }
            )
        }
        ScreenState.Register -> {
            RegisterScreen(
                authViewModel = authViewModel,
                role = selectedRole,
                onBack = { currentScreen = ScreenState.Login }
            )
        }
        ScreenState.ForgotPassword -> {
            ForgotPasswordScreen(
                onBack = { currentScreen = ScreenState.Login }
            )
        }
        ScreenState.PatientDashboard -> {
            DashboardScreen(
                patientViewModel = patientViewModel,
                onLogout = { 
                    patientViewModel.clearActiveUser()
                    sessionManager.clearSession()
                    currentScreen = ScreenState.Login 
                },
                onNavigateToEvaluation = { currentScreen = ScreenState.Evaluation },
                onNavigateToAppointments = { currentScreen = ScreenState.MyAppointments },
                onNavigateToMedications = { currentScreen = ScreenState.MyMedications },
                onNavigateToProfile = { currentScreen = ScreenState.MyProfile },
                onNavigateToExams = { currentScreen = ScreenState.MyExams }
            )
        }
        ScreenState.Evaluation -> {
            EvaluationScreen(
                patientViewModel = patientViewModel,
                onBack = { currentScreen = ScreenState.PatientDashboard }
            )
        }
        ScreenState.MyAppointments -> {
            MyAppointmentsScreen(
                patientViewModel = patientViewModel,
                onBack = { currentScreen = ScreenState.PatientDashboard }
            )
        }
        ScreenState.MyMedications -> {
            MedicationsScreen(
                patientViewModel = patientViewModel,
                onBack = { currentScreen = ScreenState.PatientDashboard }
            )
        }
        ScreenState.MyProfile -> {
            ProfileScreen(
                patientViewModel = patientViewModel,
                onBack = { currentScreen = ScreenState.PatientDashboard },
                onLogout = { 
                    patientViewModel.clearActiveUser()
                    sessionManager.clearSession()
                    currentScreen = ScreenState.Login 
                }
            )
        }
        ScreenState.MyExams -> {
            ExamsScreen(
                patientViewModel = patientViewModel,
                onBack = { currentScreen = ScreenState.PatientDashboard }
            )
        }
        ScreenState.ManagerDashboard -> {
            ManagerDashboardScreen(
                patientViewModel = patientViewModel,
                onLogout = {
                    patientViewModel.clearActiveUser()
                    sessionManager.clearSession()
                    currentScreen = ScreenState.Login
                }
            )
        }
    }
}
