package com.example.hoopmaster.network

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("hoop_prefs", Context.MODE_PRIVATE)

    fun saveUserId(userId: String) {
        prefs.edit { putString("user_id", userId) }
    }

    fun getUserId(): String? = prefs.getString("user_id", null)

    fun logout() {
        prefs.edit { clear() }
    }
}
