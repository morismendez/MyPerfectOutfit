package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.entities.UserEntity
import com.myperfectoutfit.data.local.entities.StyleRuleEntity
import com.myperfectoutfit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserUiState(
    val user: UserEntity? = null,
    val styleRules: List<StyleRuleEntity> = emptyList(),
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

    fun registerUser(
        name: String, 
        email: String, 
        profilePictureUrl: String? = null, 
        activeCategories: String
    ) {
        viewModelScope.launch {
            repository.saveUser(
                UserEntity(
                    name = name,
                    email = email,
                    profilePictureUrl = profilePictureUrl,
                    activeCategories = activeCategories
                )
            )
        }
    }

    fun updateUser(
        name: String, 
        email: String, 
        profilePictureUrl: String?, 
        activeCategories: String? = null,
        displayMode: com.myperfectoutfit.data.local.enums.CategoryDisplayMode? = null
    ) {
        viewModelScope.launch {
            val currentUser = uiState.value.user ?: return@launch
            repository.updateUser(
                currentUser.copy(
                    name = name,
                    email = email,
                    profilePictureUrl = profilePictureUrl ?: currentUser.profilePictureUrl,
                    activeCategories = activeCategories ?: currentUser.activeCategories,
                    categoryDisplayMode = displayMode ?: currentUser.categoryDisplayMode
                )
            )
        }
    }


    fun addStyleRule(title: String, description: String) {
        viewModelScope.launch {
            val userId = uiState.value.user?.id ?: return@launch
            repository.insertStyleRule(
                StyleRuleEntity(
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

    fun deleteStyleRule(rule: StyleRuleEntity) {
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
