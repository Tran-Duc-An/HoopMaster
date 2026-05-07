package com.example.hoopmaster.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoopmaster.core.di.AppContainer
import com.example.hoopmaster.data.model.PlanningChatMessageDto
import com.example.hoopmaster.data.model.TrainingPlanDto
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlanningChatEntry(
    val id: String,
    val role: String,
    val text: String,
    val planDraft: TrainingPlanDto? = null
)

data class PlanningChatUiState(
    val userId: String? = null,
    val input: String = "",
    val messages: List<PlanningChatEntry> = emptyList(),
    val draftPlan: TrainingPlanDto? = null,
    val savedPlanTitle: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val activePlanId: String? = null,
    val statusMessage: String? = null
)

sealed interface PlanningChatAction {
    data class InputChanged(val value: String) : PlanningChatAction
    data object SendMessage : PlanningChatAction
    data class ConfirmPlan(val planId: String) : PlanningChatAction
    data object LoadHistory : PlanningChatAction
    data object ClearError : PlanningChatAction
}

class PlanningChatViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AppContainer(application)
    private val planningRepository = container.planningRepository

    private val _uiState = MutableStateFlow(PlanningChatUiState())
    val uiState: StateFlow<PlanningChatUiState> = _uiState.asStateFlow()

    fun onAction(action: PlanningChatAction) {
        when (action) {
            is PlanningChatAction.InputChanged -> _uiState.update { it.copy(input = action.value) }
            PlanningChatAction.SendMessage -> sendMessage()
            is PlanningChatAction.ConfirmPlan -> _uiState.update {
                it.copy(activePlanId = action.planId, isLoading = false)
            }
            PlanningChatAction.LoadHistory -> loadHistory()
            PlanningChatAction.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            val userId = container.sessionStore.getUserId()
            if (userId.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Missing user session", isLoading = false) }
                return@launch
            }

            _uiState.update { it.copy(userId = userId, isLoading = true, errorMessage = null) }

            planningRepository.getHistory(userId).fold(
                onSuccess = { history ->
                    val items = (history.history ?: history.messages ?: history.chat).orEmpty()
                        .mapIndexed { index, message -> message.toEntry(index) }
                    _uiState.update {
                        it.copy(
                            messages = items,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load chat"
                        )
                    }
                }
            )
        }
    }

    fun sendMessage() {
        viewModelScope.launch {
            val userId = container.sessionStore.getUserId()
            val text = _uiState.value.input.trim()
            if (userId.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Missing user session") }
                return@launch
            }
            if (text.isBlank()) return@launch

            val userEntry = PlanningChatEntry(
                id = "local-${System.currentTimeMillis()}",
                role = "user",
                text = text
            )
            _uiState.update {
                it.copy(
                    userId = userId,
                    input = "",
                    messages = it.messages + userEntry,
                    isLoading = true,
                    errorMessage = null,
                    statusMessage = null
                )
            }

            planningRepository.sendMessage(userId, text).fold(
                onSuccess = { response ->
                    val draftPlan = response.planDraft ?: response.plan
                    val replyText = response.reply ?: response.message ?: "Plan updated."
                    val assistantEntry = PlanningChatEntry(
                        id = "reply-${System.currentTimeMillis()}",
                        role = "assistant",
                        text = replyText,
                        planDraft = if (response.type?.equals("plan_draft", ignoreCase = true) == true) draftPlan else null
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantEntry,
                            draftPlan = if (response.type?.equals("plan_draft", ignoreCase = true) == true) draftPlan else it.draftPlan,
                            isLoading = false,
                            errorMessage = null,
                            statusMessage = if (response.type?.equals("plan_draft", ignoreCase = true) == true) "Draft ready" else replyText
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to send message"
                        )
                    }
                }
            )
        }
    }

    fun confirmPlan() {
        viewModelScope.launch {
            val userId = container.sessionStore.getUserId()
            val planId = _uiState.value.draftPlan?.id
            if (userId.isNullOrBlank() || planId.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "No draft plan to confirm") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            planningRepository.confirmPlan(userId, planId).fold(
                onSuccess = { response ->
                    val savedPlan = response.plan ?: response.planDraft ?: _uiState.value.draftPlan
                    _uiState.update {
                        it.copy(
                            activePlanId = savedPlan?.id ?: planId,
                            savedPlanTitle = savedPlan?.title,
                            draftPlan = null,
                            isLoading = false,
                            statusMessage = savedPlan?.title?.let { title -> "Saved $title" } ?: "Plan saved"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to confirm plan"
                        )
                    }
                }
            )
        }
    }

    private fun PlanningChatMessageDto.toEntry(index: Int): PlanningChatEntry {
        val roleValue = role ?: if (type == "user") "user" else "assistant"
        return PlanningChatEntry(
            id = id ?: "${roleValue}-$index",
            role = roleValue,
            text = text ?: content ?: reply ?: messageText(),
            planDraft = if (type?.equals("plan_draft", ignoreCase = true) == true) planDraft ?: plan else null
        )
    }

    private fun PlanningChatMessageDto.messageText(): String {
        return when {
            reply != null -> reply
            text != null -> text
            content != null -> content
            planDraft?.title != null -> planDraft?.title.orEmpty()
            plan?.title != null -> plan?.title.orEmpty()
            else -> ""
        }
    }
}
