package com.example.hoopmaster.data.realtime

import com.example.hoopmaster.data.model.CoachSocketEvent
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

interface CoachSocket {
    val events: SharedFlow<CoachSocketEvent>

    fun connect(userId: String?)
    fun disconnect()
    fun sendPoseData(payload: JSONObject)
    fun startExercise(exerciseId: Int, sets: Int?, reps: Int?, restSeconds: Int?)
    fun stopExercise()
    fun sendShotReleased()
    fun requestSessionInfo()
}
