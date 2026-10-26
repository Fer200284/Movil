package com.sistema.paciencare.data.remote

import android.content.Context
import com.sistema.paciencare.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL =
        "https://paciapi-c8a0esfgbkcsf7b8.centralus-01.azurewebsites.net/api/"

    private lateinit var sessionManager: SessionManager

    fun initialize(context: Context) {
        sessionManager = SessionManager(context.applicationContext)
    }

    // =========================================================
    // LOGGING
    // =========================================================

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    // =========================================================
    // AUTENTICACIÓN
    // =========================================================

    private val authInterceptor = Interceptor { chain ->

        val originalRequest = chain.request()

        val path = originalRequest.url.encodedPath
        val method = originalRequest.method

        val isLogin =
            path.endsWith("/token/")

        val isRefresh =
            path.endsWith("/token/refresh/")

        val isRegister =
            path.endsWith("/usuarios/") &&
                    method.equals("POST", ignoreCase = true)

        val isPublicRequest =
            isLogin ||
                    isRefresh ||
                    isRegister

        if (isPublicRequest) {

            return@Interceptor chain.proceed(
                originalRequest
            )
        }

        // =====================================================
        // PETICIONES PROTEGIDAS
        // =====================================================

        val token =
            if (::sessionManager.isInitialized) {
                sessionManager.getAccessToken()
            } else {
                null
            }

        val request =
            if (!token.isNullOrBlank()) {

                originalRequest
                    .newBuilder()
                    .header(
                        "Authorization",
                        "Bearer $token"
                    )
                    .build()

            } else {

                originalRequest
            }

        chain.proceed(request)
    }

    // =========================================================
    // RENOVACIÓN AUTOMÁTICA DE TOKEN EN HTTP 401
    // =========================================================

    private val tokenAuthenticator = okhttp3.Authenticator { _, response ->
        if (response.code == 401 && ::sessionManager.isInitialized) {
            val refreshToken = sessionManager.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                synchronized(this) {
                    val currentToken = sessionManager.getAccessToken()
                    val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")

                    if (currentToken != null && currentToken != requestToken) {
                        return@Authenticator response.request.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                    }

                    try {
                        val refreshResponse = apiService.refreshTokenSync(
                            com.sistema.paciencare.data.remote.dto.RefreshTokenRequest(refreshToken)
                        ).execute()

                        if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                            val newToken = refreshResponse.body()!!.access
                            sessionManager.updateAccessToken(newToken)
                            return@Authenticator response.request.newBuilder()
                                .header("Authorization", "Bearer $newToken")
                                .build()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PACI_DEBUG", "Error authenticating token refresh: ${e.message}")
                    }
                }
            }
        }
        null
    }

    // =========================================================
    // HTTP CLIENT (MÁXIMO 15 SEGUNDOS PARA INGRESO RÁPIDO)
    // =========================================================

    private val okHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(
                45,
                TimeUnit.SECONDS
            )
            .readTimeout(
                45,
                TimeUnit.SECONDS
            )
            .writeTimeout(
                45,
                TimeUnit.SECONDS
            )
            .build()

    // =========================================================
    // RETROFIT & GSON ADAPTATIVO
    // =========================================================

    private val gson: com.google.gson.Gson = com.google.gson.GsonBuilder()
        .registerTypeHierarchyAdapter(List::class.java, FlexibleListDeserializer<Any>())
        .create()

    val apiService: PacienCareApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create(gson)
            )
            .build()
            .create(PacienCareApiService::class.java)
    }
}

class FlexibleListDeserializer<T> : com.google.gson.JsonDeserializer<List<T>> {
    override fun deserialize(
        json: com.google.gson.JsonElement,
        typeOfT: java.lang.reflect.Type,
        context: com.google.gson.JsonDeserializationContext
    ): List<T> {
        val itemType = (typeOfT as? java.lang.reflect.ParameterizedType)?.actualTypeArguments?.getOrNull(0)
            ?: return emptyList()

        if (json.isJsonArray) {
            val list = mutableListOf<T>()
            for (elem in json.asJsonArray) {
                val item: T? = context.deserialize(elem, itemType)
                if (item != null) list.add(item)
            }
            return list
        } else if (json.isJsonObject) {
            val obj = json.asJsonObject
            val arrayKeys = listOf("results", "data", "items", "pacientes", "medicos", "examenes", "medicamentos", "atenciones")
            for (key in arrayKeys) {
                if (obj.has(key) && obj.get(key).isJsonArray) {
                    val list = mutableListOf<T>()
                    for (elem in obj.getAsJsonArray(key)) {
                        val item: T? = context.deserialize(elem, itemType)
                        if (item != null) list.add(item)
                    }
                    return list
                }
            }
        }
        return emptyList()
    }
}
