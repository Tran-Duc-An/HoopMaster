package com.example.hoopmaster.core.di

import android.content.Context
import com.example.hoopmaster.core.config.AppConfig
import com.example.hoopmaster.core.config.DemoModeConfig
import com.example.hoopmaster.core.session.SessionStore
import com.example.hoopmaster.data.api.HoopMasterApi
import com.example.hoopmaster.data.demo.DemoAuthRepository
import com.example.hoopmaster.data.demo.DemoCoachSocketClient
import com.example.hoopmaster.data.demo.DemoExerciseRepository
import com.example.hoopmaster.data.demo.DemoPlanRepository
import com.example.hoopmaster.data.demo.DemoPlanningRepository
import com.example.hoopmaster.data.realtime.CoachSocket
import com.example.hoopmaster.data.realtime.CoachSocketClient
import com.example.hoopmaster.data.repository.AuthDataSource
import com.example.hoopmaster.data.repository.AuthRepository
import com.example.hoopmaster.data.repository.ExerciseDataSource
import com.example.hoopmaster.data.repository.ExerciseRepository
import com.example.hoopmaster.data.repository.PlanDataSource
import com.example.hoopmaster.data.repository.PlanRepository
import com.example.hoopmaster.data.repository.PlanningDataSource
import com.example.hoopmaster.data.repository.PlanningRepository
import com.example.hoopmaster.network.RetrofitClient

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sessionStore: SessionStore = SessionStore(appContext)
    val api: HoopMasterApi = RetrofitClient.hoopMasterApi

    val authRepository: AuthDataSource = if (DemoModeConfig.enabled) {
        DemoAuthRepository(sessionStore)
    } else {
        AuthRepository(api, sessionStore)
    }

    val planningRepository: PlanningDataSource = if (DemoModeConfig.enabled) {
        DemoPlanningRepository()
    } else {
        PlanningRepository(api)
    }

    val planRepository: PlanDataSource = if (DemoModeConfig.enabled) {
        DemoPlanRepository()
    } else {
        PlanRepository(api)
    }

    val exerciseRepository: ExerciseDataSource = if (DemoModeConfig.enabled) {
        DemoExerciseRepository()
    } else {
        ExerciseRepository(api)
    }

    fun createSocketClient(): CoachSocket = if (DemoModeConfig.enabled) {
        DemoCoachSocketClient()
    } else {
        CoachSocketClient(AppConfig.SOCKET_URL)
    }
}
