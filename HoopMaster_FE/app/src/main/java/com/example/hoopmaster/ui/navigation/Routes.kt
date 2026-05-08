package com.example.hoopmaster.ui.navigation

object Routes {
    const val Login = "login"
    const val Planning = "planning"
    const val Home = "home"
    const val ExerciseDetailArg = "exerciseId"
    const val ExerciseDetail = "exercise/{$ExerciseDetailArg}"
    const val Tracking = "tracking"
    const val TrackingExerciseArg = "exerciseId"
    const val TrackingWithExercise = "tracking/{$TrackingExerciseArg}"
    const val SummarySocketIdArg = "socketId"
    const val Summary = "summary?$SummarySocketIdArg={$SummarySocketIdArg}"
    const val Profile = "profile"

    fun exerciseDetail(exerciseId: Int): String = "exercise/$exerciseId"
    fun trackingWithExercise(exerciseId: Int): String = "tracking/$exerciseId"
    fun summary(socketId: String?): String =
        if (socketId.isNullOrBlank()) {
            "summary"
        } else {
            "summary?$SummarySocketIdArg=$socketId"
        }
}
