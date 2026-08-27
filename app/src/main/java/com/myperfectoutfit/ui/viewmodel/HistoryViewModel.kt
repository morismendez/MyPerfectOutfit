package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.dao.HistoryWithDetails
import com.myperfectoutfit.data.local.entities.CustomGarmentEntity
import com.myperfectoutfit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class HistoryUiState(
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val outfitsForDate: List<HistoryWithDetails> = emptyList(),
    val customGarments: List<CustomGarmentEntity> = emptyList(),
    val allHistory: List<HistoryWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: WardrobeRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getPrimaryUser(),
        _selectedDate
    ) { user, date -> 
        user to date 
    }.flatMapLatest { (user, date) ->
        if (user == null) flowOf(HistoryUiState(isLoading = false))
        else {
            combine(
                repository.getFullHistory(user.id),
                repository.getHistoryByDate(user.id, date),
                repository.getAllCustomGarments(user.id)
            ) { full, forDate, custom ->
                HistoryUiState(
                    selectedDate = date,
                    allHistory = full,
                    outfitsForDate = forDate,
                    customGarments = custom,
                    isLoading = false
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun onDateSelected(date: String) {
        _selectedDate.value = date
    }
}
