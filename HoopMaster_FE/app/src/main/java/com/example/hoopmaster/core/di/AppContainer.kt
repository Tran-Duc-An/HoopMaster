package com.example.hoopmaster.core.di

import android.content.Context
import com.example.hoopmaster.core.session.SessionStore
import com.example.hoopmaster.data.api.HoopMasterApi
import com.example.hoopmaster.data.realtime.CoachSocketClient
import com.example.hoopmaster.data.repository.AuthRepository
import com.example.hoopmaster.data.repository.ExerciseRepository
import com.example.hoopmaster.data.repository.PlanRepository
import com.example.hoopmaster.data.repository.PlanningRepository
import com.example.hoopmaster.core.config.AppConfig
import com.example.hoopmaster.network.RetrofitClient

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sessionStore: SessionStore = SessionStore(appContext)
    val api: HoopMasterApi = RetrofitClient.hoopMasterApi

    val authRepository: AuthRepository = AuthRepository(api, sessionStore)
    val planningRepository: PlanningRepository = PlanningRepository(api)
    val planRepository: PlanRepository = PlanRepository(api)
    val exerciseRepository: ExerciseRepository = ExerciseRepository(api)

    fun createSocketClient(): CoachSocketClient = CoachSocketClient(AppConfig.SOCKET_URL)
}
