package com.example.hoopmaster.data.demo

import com.example.hoopmaster.core.session.SessionStore
import com.example.hoopmaster.data.model.CoachTone
import com.example.hoopmaster.data.model.UserDto
import com.example.hoopmaster.data.repository.AuthDataSource

class DemoAuthRepository(
    private val sessionStore: SessionStore
) : AuthDataSource {
    private var currentUser: UserDto = DemoFixtures.user()

    override suspend fun login(usernameOrEmail: String, password: String): Result<UserDto> {
        val demoUser = DemoFixtures.user()
        currentUser = demoUser.copy(
            username = usernameOrEmail.substringBefore("@").ifBlank { demoUser.username },
            email = if ("@" in usernameOrEmail) usernameOrEmail else demoUser.email
        )
        sessionStore.saveUser(currentUser)
        currentUser.tone?.let { sessionStore.saveTone(it) }
        return Result.success(currentUser)
    }

    override suspend fun signup(
        username: String,
        email: String,
        password: String,
        name: String?
    ): Result<UserDto> {
        currentUser = DemoFixtures.user().copy(
            username = username,
            email = email,
            name = name ?: DemoFixtures.user().name
        )
        sessionStore.saveUser(currentUser)
        currentUser.tone?.let { sessionStore.saveTone(it) }
        return Result.success(currentUser)
    }

    override suspend fun updateTone(userId: String, tone: CoachTone): Result<UserDto> {
        currentUser = currentUser.copy(
            id = if (currentUser.id.isNullOrBlank()) userId else currentUser.id,
            tone = tone
        )
        sessionStore.saveUser(currentUser)
        sessionStore.saveTone(tone)
        return Result.success(currentUser)
    }

    override fun logout() {
        sessionStore.clear()
    }
}
