package com.example.hoopmaster.data.model

// Request gửi khi log workout
data class LogWorkoutRequestDto(
    val exerciseId: Int,
    val name: String? = null,
    val category: String? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val durationMinutes: Int? = null
)

// Response khi log workout
// Có thể mở rộng theo BE trả về
data class WorkoutHistoryLogResponseDto(
    val _id: String?,
    val userId: String?,
    val date: String?,
    val totalExercises: Int?,
    val totalSets: Int?,
    val totalReps: Int?,
    val totalMinutes: Int?,
    val exercises: List<WorkoutExerciseDto>?
)

data class WorkoutExerciseDto(
    val exerciseId: Int?,
    val name: String?,
    val category: String?,
    val sets: Int?,
    val reps: Int?,
    val durationMinutes: Int?
)

// Response cho getWeeklyWorkoutHistory
// days là mảng 7 ngày gần nhất
// Có thể mở rộng theo BE trả về

data class WorkoutHistoryWeeklyResponseDto(
    val days: List<WorkoutHistoryDayDto>
)

data class WorkoutHistoryDayDto(
    val date: String,
    val dayLabel: String,
    val totalExercises: Int,
    val totalMinutes: Int,
    val totalSets: Int
)

// Response cho getAllWorkoutHistory
// history là mảng các ngày

data class WorkoutHistoryListResponseDto(
    val history: List<WorkoutHistoryLogResponseDto>
)
