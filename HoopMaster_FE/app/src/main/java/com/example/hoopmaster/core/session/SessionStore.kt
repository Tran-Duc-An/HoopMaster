package com.example.hoopmaster.core.session

import android.content.Context
import androidx.core.content.edit
import com.example.hoopmaster.data.model.CoachTone
import com.example.hoopmaster.data.model.UserDto

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUser(user: UserDto) {
        val userId = user.id ?: return
        prefs.edit {
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            putString(KEY_NAME, user.name)
            putString(KEY_TONE, user.tone?.backendValue())
        }
    }

    fun saveTone(tone: CoachTone) {
        prefs.edit { putString(KEY_TONE, tone.backendValue()) }
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getTone(): CoachTone = prefs.getString(KEY_TONE, null).toCoachTone()

    fun isLoggedIn(): Boolean = getUserId() != null

    fun clear() {
        prefs.edit { clear() }
    }

    private fun String?.toCoachTone(): CoachTone = when (this?.lowercase()) {
        "strict" -> CoachTone.STRICT
        "cheerful" -> CoachTone.CHEERFUL
        else -> CoachTone.NEUTRAL
    }

    private companion object {
        const val PREFS_NAME = "hoop_prefs"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_TONE = "tone"
    }
}
