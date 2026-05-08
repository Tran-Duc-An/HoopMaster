package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.model.TrainingPlanDto

interface PlanDataSource {
    suspend fun getActivePlan(userId: String): Result<TrainingPlanDto?>
    suspend fun getPlans(
        userId: String,
        source: String? = null,
        status: String? = null
    ): Result<List<TrainingPlanDto>>
    suspend fun getDefaultPlan(): Result<TrainingPlanDto>
}
