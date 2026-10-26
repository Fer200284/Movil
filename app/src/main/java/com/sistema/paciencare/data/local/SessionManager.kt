package com.sistema.paciencare.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_GENDER = "user_gender"
        private const val KEY_USER_ROLE = "user_role"

        // JWT
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    fun saveSession(
        isLoggedIn: Boolean,
        name: String,
        isFemale: Boolean,
        role: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null,
        email: String? = null
    ) {
        prefs.edit().apply {

            putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            putString(KEY_USER_NAME, name)
            putBoolean(KEY_USER_GENDER, isFemale)

            if (email != null) {
                putString(KEY_USER_EMAIL, email)
            }

            if (role != null) {
                putString(KEY_USER_ROLE, role)
            }

            if (accessToken != null) {
                putString(KEY_ACCESS_TOKEN, accessToken)
            }

            if (refreshToken != null) {
                putString(KEY_REFRESH_TOKEN, refreshToken)
            }

            commit()
        }
    }

    fun isLoggedIn(): Boolean =
        prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserName(): String? =
        prefs.getString(KEY_USER_NAME, null)

    fun getUserEmail(): String? =
        prefs.getString(KEY_USER_EMAIL, null)

    fun isFemale(): Boolean =
        prefs.getBoolean(KEY_USER_GENDER, false)

    fun getUserRole(): String? =
        prefs.getString(KEY_USER_ROLE, null)

    fun getAccessToken(): String? =
        prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? =
        prefs.getString(KEY_REFRESH_TOKEN, null)

    fun updateAccessToken(accessToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
    }

    // =========================================================
    // REGISTRO DE ELEMENTOS ELIMINADOS LOCALMENTE (PERSISTENCIA)
    // =========================================================

    fun getDeletedAppointmentIds(email: String): Set<String> {
        return prefs.getStringSet("deleted_appts_$email", emptySet()) ?: emptySet()
    }

    fun addDeletedAppointmentId(email: String, id: String) {
        val current = getDeletedAppointmentIds(email).toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("deleted_appts_$email", current).apply()
    }

    fun getDeletedMedicationIds(email: String): Set<String> {
        return prefs.getStringSet("deleted_meds_$email", emptySet()) ?: emptySet()
    }

    fun addDeletedMedicationId(email: String, id: String) {
        val current = getDeletedMedicationIds(email).toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("deleted_meds_$email", current).apply()
    }

    fun getDeletedExamIds(email: String): Set<String> {
        return prefs.getStringSet("deleted_exams_$email", emptySet()) ?: emptySet()
    }

    fun addDeletedExamId(email: String, id: String) {
        val current = getDeletedExamIds(email).toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("deleted_exams_$email", current).apply()
    }

    fun getDeletedEvaluationIds(email: String): Set<String> {
        return prefs.getStringSet("deleted_evals_$email", emptySet()) ?: emptySet()
    }

    fun addDeletedEvaluationId(email: String, id: String) {
        val current = getDeletedEvaluationIds(email).toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("deleted_evals_$email", current).apply()
    }

    fun clearAllDeletedItemBlacklists() {
        val editor = prefs.edit()
        prefs.all.keys.filter { 
            it.startsWith("deleted_appts_") || 
            it.startsWith("deleted_meds_") || 
            it.startsWith("deleted_exams_") || 
            it.startsWith("deleted_evals_") 
        }.forEach { key ->
            editor.remove(key)
        }
        editor.apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
