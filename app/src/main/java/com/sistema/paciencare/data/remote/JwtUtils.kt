package com.sistema.paciencare.data.remote

import android.util.Base64
import org.json.JSONObject

object JwtUtils {

    fun getUserIdFromToken(token: String): Int? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payloadBase64 = parts[1]
            val decodedBytes = Base64.decode(payloadBase64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val jsonString = String(decodedBytes, Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)

            if (jsonObject.has("user_id")) {
                jsonObject.getInt("user_id")
            } else if (jsonObject.has("id")) {
                jsonObject.getInt("id")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
