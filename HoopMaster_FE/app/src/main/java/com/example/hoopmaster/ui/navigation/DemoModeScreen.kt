package com.example.hoopmaster.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hoopmaster.data.model.ExerciseScriptItemDto
import com.example.hoopmaster.data.model.ExerciseTargetDto
import com.example.hoopmaster.data.model.ExerciseVoiceCuesDto
import com.example.hoopmaster.data.model.ExerciseVoiceScriptDto
import com.example.hoopmaster.data.model.PlanExerciseDto
import com.example.hoopmaster.data.model.PlanMetadataDto
import com.example.hoopmaster.data.model.TrainingPlanDto
import com.example.hoopmaster.ui.screens.ExerciseDetailScreen
import com.example.hoopmaster.ui.screens.HomeScreen
import com.example.hoopmaster.ui.screens.LoginScreen
import com.example.hoopmaster.ui.screens.PlanningChatScreen
import com.example.hoopmaster.ui.screens.ProfileScreen
import com.example.hoopmaster.ui.screens.SessionSummaryScreen
import com.example.hoopmaster.ui.screens.TrackingScreen
import com.example.hoopmaster.viewmodels.AuthUiState
import com.example.hoopmaster.viewmodels.ExerciseDetailUiState
import com.example.hoopmaster.viewmodels.HomeUiState
import com.example.hoopmaster.viewmodels.PlanningChatEntry
import com.example.hoopmaster.viewmodels.PlanningChatUiState
import com.example.hoopmaster.viewmodels.ProfileUiState
import com.example.hoopmaster.viewmodels.SessionSummaryUiState
import com.example.hoopmaster.viewmodels.TrackingUiState

enum class DemoScreen(
    val title: String,
    val description: String
) {
    Login("Login", "Auth form states"),
    Home("Home", "Plan + exercise list"),
    Planning("Planning", "Chat + draft plan"),
    ExerciseDetail("Exercise detail", "Targets, steps, warnings"),
    Tracking("Tracking", "Live workout overlay"),
    Summary("Summary", "Post-session recap"),
    Profile("Profile", "Tone + session profile")
}

@Composable
fun DemoModeScreen(
    onExit: () -> Unit,
    initialScreen: DemoScreen = DemoModeConfig.startScreen,
    showPicker: Boolean = DemoModeConfig.showScreenPicker
) {
    var selectedScreenName by rememberSaveable {
        mutableStateOf(if (showPicker) null else initialScreen.name)
    }
    val selectedScreen = selectedScreenName?.let(DemoScreen::valueOf)

    if (selectedScreen == null) {
        DemoScreenPicker(
            onExit = onExit,
            onOpen = { selectedScreenName = it.name }
        )
        return
    }

    when (selectedScreen) {
        DemoScreen.Login -> LoginScreen(
            onLoginSuccess = { selectedScreenName = DemoScreen.Home.name },
            demoState = DemoSamples.loginState,
            demoSignupMode = false
        )

        DemoScreen.Home -> HomeScreen(
            onPersonalizePlan = { selectedScreenName = DemoScreen.Planning.name },
            onStartShooting = { selectedScreenName = DemoScreen.Tracking.name },
            onOpenExercise = { selectedScreenName = DemoScreen.ExerciseDetail.name },
            onOpenProfile = { selectedScreenName = DemoScreen.Profile.name },
            demoState = DemoSamples.homeState
        )

        DemoScreen.Planning -> PlanningChatScreen(
            onBack = { selectedScreenName = DemoScreen.Home.name },
            demoState = DemoSamples.planningState
        )

        DemoScreen.ExerciseDetail -> ExerciseDetailScreen(
            exerciseId = 101,
            onBack = { selectedScreenName = DemoScreen.Home.name },
            onStartTracking = { selectedScreenName = DemoScreen.Tracking.name },
            demoState = DemoSamples.exerciseDetailState
        )

        DemoScreen.Tracking -> TrackingScreen(
            onEndSession = { selectedScreenName = DemoScreen.Summary.name },
            demoState = DemoSamples.trackingState,
            demoHasCameraPermission = false
        )

        DemoScreen.Summary -> SessionSummaryScreen(
            onBackHome = { selectedScreenName = DemoScreen.Home.name },
            demoState = DemoSamples.summaryState
        )

        DemoScreen.Profile -> ProfileScreen(
            onBack = { selectedScreenName = DemoScreen.Home.name },
            onLogout = { selectedScreenName = DemoScreen.Login.name },
            demoState = DemoSamples.profileState
        )
    }
}

@Composable
private fun DemoScreenPicker(
    onExit: () -> Unit,
    onOpen: (DemoScreen) -> Unit
) {
    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                    Text("Exit demo mode")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Demo mode",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Open any screen with seeded UI data. No auth, no network, no camera needed.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(DemoScreen.entries) { screen ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpen(screen) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = screen.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private object DemoSamples {
    val loginState = AuthUiState(
        email = "demo@hoopmaster.ai",
        password = "password123"
    )

    private val sampleExercises = listOf(
        PlanExerciseDto(
            exerciseId = 101,
            name = "Form Shooting",
            category = "Shooting",
            description = "Close-range reps for alignment, wrist snap, soft touch.",
            sets = 4,
            reps = 12,
            target = ExerciseTargetDto(restSeconds = 30)
        ),
        PlanExerciseDto(
            exerciseId = 102,
            name = "Corner Catch-and-Shoot",
            category = "Game reps",
            description = "Sprint into corner spacing, square feet fast, release on balance.",
            sets = 5,
            reps = 8,
            target = ExerciseTargetDto(restSeconds = 45)
        )
    )

    val homeState = HomeUiState(
        activePlanTitle = "Sharpshooter Week 2",
        activePlanDescription = "Build repeatable mechanics, faster release, stronger off-ball footwork.",
        exercises = sampleExercises
    )

    private val samplePlan = TrainingPlanDto(
        id = "plan-demo-1",
        title = "Sharpshooter Week 2",
        description = "3 sessions focused on arc consistency and game-speed footwork.",
        goal = "Raise catch-and-shoot accuracy",
        exercises = sampleExercises,
        metadata = PlanMetadataDto(source = "demo", status = "draft")
    )

    val planningState = PlanningChatUiState(
        input = "Add more movement shooting",
        messages = listOf(
            PlanningChatEntry(
                id = "1",
                role = "assistant",
                text = "Tell me goal for this week."
            ),
            PlanningChatEntry(
                id = "2",
                role = "user",
                text = "Need faster release off catch."
            ),
            PlanningChatEntry(
                id = "3",
                role = "assistant",
                text = "Draft ready. Focus on feet first, then arc.",
                planDraft = samplePlan
            )
        ),
        draftPlan = samplePlan,
        statusMessage = "Draft ready"
    )

    val exerciseDetailState = ExerciseDetailUiState(
        exerciseId = 101,
        title = "Form Shooting",
        category = "Shooting",
        description = "One-hand guide-free reps from short range to lock elbow alignment and touch.",
        steps = listOf(
            "Start one step from rim, square shoulders.",
            "Hold follow-through until ball hits net.",
            "Track arc and wrist snap every rep."
        ),
        warnings = listOf(
            "Do not rush release before full balance.",
            "Stop if wrist pain appears."
        ),
        sets = 4,
        reps = 12,
        restSeconds = 30
    )

    val trackingState = TrackingUiState(
        feedbackText = "Good arc. Release 0.62s. Keep elbow under ball.",
        selectedTone = "strict",
        isConnected = true,
        isExerciseActive = true
    )

    val summaryState = SessionSummaryUiState(
        sessionId = "session-demo-1",
        lastFeedback = "Best reps came when hips stayed square through release.",
        totalShots = 42,
        madeShots = 31,
        durationSeconds = 1080,
        highlight = "Accuracy up 9% from last session."
    )

    val profileState = ProfileUiState(
        userId = "demo-user",
        displayName = "Demo Athlete",
        email = "demo@hoopmaster.ai",
        tone = "strict"
    )

    @Suppress("unused")
    val sampleVoiceScript = ExerciseVoiceScriptDto(
        exerciseId = 101,
        name = "Form Shooting",
        target = ExerciseTargetDto(sets = 4, reps = 12, restSeconds = 30),
        warnings = listOf("Keep landing soft."),
        script = listOf(
            ExerciseScriptItemDto(type = "intro", text = "Focus on clean wrist snap."),
            ExerciseScriptItemDto(type = "rep", text = "Hold follow-through.")
        )
    )

    @Suppress("unused")
    val sampleVoiceCues = ExerciseVoiceCuesDto(
        intro = "Slow down first reps.",
        setup = "Feet under hips.",
        warnings = listOf("Do not fade left.")
    )
}
