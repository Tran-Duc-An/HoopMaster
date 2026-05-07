package com.example.hoopmaster.data.demo

import com.example.hoopmaster.data.model.TrainingPlanDto
import com.example.hoopmaster.data.repository.PlanDataSource

class DemoPlanRepository : PlanDataSource {
    override suspend fun getActivePlan(userId: String): Result<TrainingPlanDto?> {
        return Result.success(DemoFixtures.activePlan().copy(userId = userId))
    }

    override suspend fun getPlans(
        userId: String,
        source: String?,
        status: String?
    ): Result<List<TrainingPlanDto>> {
        val filtered = DemoFixtures.plans().map { it.copy(userId = userId) }
            .filter { plan ->
                (source == null || plan.source.equals(source, ignoreCase = true)) &&
                    (status == null || plan.status.equals(status, ignoreCase = true))
            }
        return Result.success(filtered)
    }

    override suspend fun getDefaultPlan(): Result<TrainingPlanDto> {
        return Result.success(DemoFixtures.activePlan().copy(id = "demo-plan-default", status = "template"))
    }
}
