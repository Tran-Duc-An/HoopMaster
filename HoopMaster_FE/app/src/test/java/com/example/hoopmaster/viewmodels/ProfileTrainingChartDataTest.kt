package com.example.hoopmaster.viewmodels

import com.example.hoopmaster.data.model.WorkoutHistoryDayDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTrainingChartDataTest {
    @Test
    fun `buildWeeklyTrainingChartData converts minutes to hours and summary labels`() {
        val days = listOf(
            WorkoutHistoryDayDto("2026-05-06", "Wed", totalExercises = 1, totalMinutes = 30, totalSets = 2),
            WorkoutHistoryDayDto("2026-05-07", "Thu", totalExercises = 1, totalMinutes = 90, totalSets = 3),
            WorkoutHistoryDayDto("2026-05-08", "Fri", totalExercises = 0, totalMinutes = 0, totalSets = 0)
        )

        val chartData = buildWeeklyTrainingChartData(days)

        assertEquals("2h total", chartData.totalLabel)
        assertEquals(1.5f, chartData.maxHours, 0.001f)
        assertEquals(0.5f, chartData.days[0].hours, 0.001f)
        assertEquals(1.5f, chartData.days[1].hours, 0.001f)
        assertEquals(0f, chartData.days[2].hours, 0.001f)
        assertEquals("Wed", chartData.days[0].label)
    }

    @Test
    fun `buildWeeklyTrainingChartData returns zero summary for empty history`() {
        val chartData = buildWeeklyTrainingChartData(emptyList())

        assertEquals("0h total", chartData.totalLabel)
        assertEquals(1f, chartData.maxHours, 0.001f)
        assertEquals(emptyList<ProfileTrainingChartDay>(), chartData.days)
    }
}
