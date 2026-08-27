package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.entities.UserEntity
import com.myperfectoutfit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserUiState(
    val user: com.myperfectoutfit.data.local.entities.UserEntity? = null,
    val styleRules: List<com.myperfectoutfit.data.local.entities.StyleRuleEntity> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: WardrobeRepository
) : ViewModel() {

    val uiState: StateFlow<UserUiState> = repository.getPrimaryUser()
        .flatMapLatest { user ->
            if (user == null) flowOf(UserUiState(isLoading = false))
            else repository.getStyleRules(user.id).map { rules ->
                UserUiState(user = user, styleRules = rules, isLoading = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserUiState()
        )

    fun registerUser(name: String, email: String, profilePictureUrl: String? = null, activeCategories: String, geminiApiKey: String? = null) {
        viewModelScope.launch {
            repository.saveUser(
                UserEntity(
                    name = name,
                    email = email,
                    profilePictureUrl = profilePictureUrl,
                    activeCategories = activeCategories
                )
            )
            geminiApiKey?.let { repository.saveGeminiApiKey(it) }
        }
    }

    fun updateUser(name: String, email: String, profilePictureUrl: String?, activeCategories: String? = null, geminiApiKey: String? = null) {
        viewModelScope.launch {
            val currentUser = uiState.value.user ?: return@launch
            repository.saveUser(
                currentUser.copy(
                    name = name,
                    email = email,
                    profilePictureUrl = profilePictureUrl ?: currentUser.profilePictureUrl,
                    activeCategories = activeCategories ?: currentUser.activeCategories
                )
            )
            geminiApiKey?.let { repository.saveGeminiApiKey(it) }
        }
    }
    
    fun getGeminiApiKey(): String? = repository.getGeminiApiKey()

    fun addStyleRule(title: String, description: String) {
        viewModelScope.launch {
            val userId = uiState.value.user?.id ?: return@launch
            repository.insertStyleRule(
                com.myperfectoutfit.data.local.entities.StyleRuleEntity(
                    userId = userId,
                    title = title,
                    description = description
                )
            )
        }
    }

    fun toggleStyleRule(ruleId: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleStyleRule(ruleId, isActive)
        }
    }

    fun deleteStyleRule(rule: com.myperfectoutfit.data.local.entities.StyleRuleEntity) {
        viewModelScope.launch {
            repository.deleteStyleRule(rule)
        }
    }

    suspend fun createBackupFile(): java.io.File? {
        return repository.createBackup()
    }

    suspend fun restoreBackupFile(uri: android.net.Uri): Boolean {
        return repository.restoreBackup(uri)
    }
}
