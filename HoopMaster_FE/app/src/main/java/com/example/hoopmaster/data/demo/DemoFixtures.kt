package com.example.hoopmaster.data.demo

import com.example.hoopmaster.data.model.CoachTone
import com.example.hoopmaster.data.model.ExerciseCountingDto
import com.example.hoopmaster.data.model.ExerciseCounterDto
import com.example.hoopmaster.data.model.ExerciseDto
import com.example.hoopmaster.data.model.ExercisePhaseDto
import com.example.hoopmaster.data.model.ExerciseScriptItemDto
import com.example.hoopmaster.data.model.ExerciseTargetDto
import com.example.hoopmaster.data.model.ExerciseTrackingDto
import com.example.hoopmaster.data.model.ExerciseVoiceCuesDto
import com.example.hoopmaster.data.model.ExerciseVoiceScriptDto
import com.example.hoopmaster.data.model.PlanExerciseDto
import com.example.hoopmaster.data.model.PlanMetadataDto
import com.example.hoopmaster.data.model.PlanningChatMessageDto
import com.example.hoopmaster.data.model.PlanningChatResponseDto
import com.example.hoopmaster.data.model.TrainingPlanDto
import com.example.hoopmaster.data.model.TrainingPlanScheduleDto
import com.example.hoopmaster.data.model.UserDto

object DemoFixtures {
    const val userId: String = "demo-user"

    private val demoUser = UserDto(
        id = userId,
        username = "demo",
        email = "demo@hoopmaster.app",
        name = "Demo Player",
        tone = CoachTone.NEUTRAL,
        token = "demo-token"
    )

    private val demoExercises = listOf(
        ExerciseDto(
            id = 1,
            name = "Form Shooting",
            category = "shooting",
            pose = "standing",
            description = "Close-range form reps with clean release.",
            sets = 3,
            reps = 10,
            target = ExerciseTargetDto(sets = 3, reps = 10, restSeconds = 45),
            counting = ExerciseCountingDto(
                mode = "reps",
                countOnPhase = "release",
                phases = listOf(
                    ExercisePhaseDto(key = "set", cue = "Set your feet", durationMs = 1500),
                    ExercisePhaseDto(key = "release", cue = "High follow-through", durationMs = 1000, countRep = true)
                )
            ),
            voiceCues = ExerciseVoiceCuesDto(
                intro = "Let's lock in form.",
                setup = "Square hips and shoulders.",
                repTemplate = "Rep {rep}. Hold your follow-through.",
                complete = "Great work."
            ),
            tracking = ExerciseTrackingDto(
                type = "pose",
                primaryJoints = listOf("right_wrist", "right_elbow", "right_shoulder"),
                counter = ExerciseCounterDto(joint = "right_elbow", downThreshold = 75.0, upThreshold = 145.0)
            )
        ),
        ExerciseDto(
            id = 2,
            name = "Catch and Shoot",
            category = "shooting",
            pose = "standing",
            description = "Quick dip into balanced release.",
            sets = 4,
            reps = 8,
            target = ExerciseTargetDto(sets = 4, reps = 8, restSeconds = 60)
        )
    )

    private val demoActivePlan = TrainingPlanDto(
        id = "demo-plan-active",
        userId = demoUser.id,
        title = "Demo Shooting Week",
        description = "Simple progression for demo mode.",
        goal = "Improve shot consistency",
        source = "ai",
        status = "active",
        schedule = TrainingPlanScheduleDto(daysPerWeek = 3, sessionDurationMinutes = 30),
        exercises = demoExercises.mapIndexed { idx, exercise ->
            PlanExerciseDto(
                exerciseId = exercise.id,
                exercise = exercise,
                name = exercise.name,
                category = exercise.category,
                pose = exercise.pose,
                description = exercise.description,
                order = idx + 1,
                sets = exercise.target?.sets ?: exercise.sets,
                reps = exercise.target?.reps ?: exercise.reps,
                target = exercise.target,
                counting = exercise.counting,
                tracking = exercise.tracking,
                voiceCues = exercise.voiceCues
            )
        },
        metadata = PlanMetadataDto(source = "demo", status = "active", tone = CoachTone.NEUTRAL)
    )

    fun user(tone: CoachTone = CoachTone.NEUTRAL): UserDto = demoUser.copy(tone = tone)

    fun activePlan(): TrainingPlanDto = demoActivePlan

    fun plans(): List<TrainingPlanDto> = listOf(demoActivePlan)

    fun exercises(): List<ExerciseDto> = demoExercises

    fun exercise(id: Int): ExerciseDto? = demoExercises.firstOrNull { it.id == id }

    fun voiceScript(
        exerciseId: Int,
        sets: Int?,
        reps: Int?,
        restSeconds: Int?
    ): ExerciseVoiceScriptDto {
        val exercise = exercise(exerciseId) ?: ExerciseDto(
            id = exerciseId,
            name = "Demo Exercise",
            category = "shooting",
            target = ExerciseTargetDto(sets = sets, reps = reps, restSeconds = restSeconds)
        )
        val target = exercise.target ?: ExerciseTargetDto()
        val resolvedTarget = target.copy(
            sets = sets ?: target.sets ?: exercise.sets,
            reps = reps ?: target.reps ?: exercise.reps,
            restSeconds = restSeconds ?: target.restSeconds
        )
        val sets = exercise.target?.sets ?: exercise.sets ?: 3
        val reps = exercise.target?.reps ?: exercise.reps ?: 10
        return ExerciseVoiceScriptDto(
            exerciseId = exercise.id,
            name = exercise.name,
            category = exercise.category,
            pose = exercise.pose,
            target = resolvedTarget,
            warnings = listOf("Land softly", "Keep knee alignment"),
            script = listOf(
                ExerciseScriptItemDto(type = "intro", text = "Starting ${exercise.name}."),
                ExerciseScriptItemDto(type = "setup", text = "Stay balanced and relaxed."),
                ExerciseScriptItemDto(type = "rep", text = "Smooth release and hold.", set = 1, rep = 1),
                ExerciseScriptItemDto(type = "complete", text = "Session complete.")
            ),
            raw = mapOf("sets" to sets, "reps" to reps)
        )
    }

    fun planningHistory(): List<PlanningChatMessageDto> {
        return listOf(
            PlanningChatMessageDto(
                id = "hist-1",
                role = "assistant",
                type = "message",
                reply = "What is your current goal?"
            ),
            PlanningChatMessageDto(
                id = "hist-2",
                role = "user",
                type = "user",
                text = "Improve my jump shot."
            ),
            PlanningChatMessageDto(
                id = "hist-3",
                role = "assistant",
                type = "plan_draft",
                reply = "I drafted a 3-day shooting plan.",
                planDraft = demoActivePlan.copy(id = "demo-plan-draft", status = "draft")
            )
        )
    }

    fun planningReply(text: String): PlanningChatResponseDto {
        val lower = text.lowercase()
        val draft = demoActivePlan.copy(id = "demo-plan-draft", userId = userId, status = "draft")
        return if (lower.contains("plan") || lower.contains("draft") || lower.contains("goal")) {
            PlanningChatResponseDto(
                type = "plan_draft",
                reply = "Draft ready. Review and confirm.",
                planDraft = draft,
                status = "ok"
            )
        } else {
            PlanningChatResponseDto(
                type = "message",
                reply = "Got it. Tell me training days per week or ask for a draft.",
                status = "ok"
            )
        }
    }

    fun confirmedPlan(planId: String): PlanningChatResponseDto {
        return PlanningChatResponseDto(
            type = "plan_confirmed",
            reply = "Plan saved.",
            plan = demoActivePlan.copy(id = planId, status = "active"),
            status = "ok"
        )
    }
}
