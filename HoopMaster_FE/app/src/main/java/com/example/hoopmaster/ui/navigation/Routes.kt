package com.example.hoopmaster.ui.navigation

object Routes {
    const val Login = "login"
    const val Planning = "planning"
    const val Home = "home"
    const val ExerciseDetailArg = "exerciseId"
    const val ExerciseDetail = "exercise/{$ExerciseDetailArg}"
    const val Tracking = "tracking"
    const val Summary = "summary"
    const val Profile = "profile"

    fun exerciseDetail(exerciseId: Int): String = "exercise/$exerciseId"
}
