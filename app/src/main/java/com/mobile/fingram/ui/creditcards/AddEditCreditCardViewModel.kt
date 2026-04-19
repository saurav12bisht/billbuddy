package com.mobile.fingram.ui.creditcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.fingram.domain.model.CreditCard
import com.mobile.fingram.domain.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddEditCreditCardUiState {
    object Idle : AddEditCreditCardUiState()
    object Loading : AddEditCreditCardUiState()
    object Saved : AddEditCreditCardUiState()
    data class Error(val message: String) : AddEditCreditCardUiState()
}

@HiltViewModel
class AddEditCreditCardViewModel @Inject constructor(
    private val repository: CreditCardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddEditCreditCardUiState>(AddEditCreditCardUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _selectedColorHex = MutableStateFlow("#B39DDB") // Default
    val selectedColorHex = _selectedColorHex.asStateFlow()

    fun updateColor(hex: String) {
        _selectedColorHex.value = hex
    }

    fun loadCreditCard(id: Long) = viewModelScope.launch {
        // Logic to load card if needed (can be done in fragment too)
    }

    suspend fun getCreditCard(id: Long): CreditCard? {
        return repository.getCreditCardById(id)
    }

    fun saveCreditCard(
        id: Long = 0,
        cardName: String,
        bankName: String,
        lastFour: String,
        billingDay: Int,
        dueDay: Int,
        colorHex: String
    ) = viewModelScope.launch {
        _uiState.value = AddEditCreditCardUiState.Loading
        try {
            val card = CreditCard(
                id = id,
                cardName = cardName,
                bankName = bankName,
                lastFourDigits = lastFour,
                billingDay = billingDay,
                dueDay = dueDay,
                colorHex = colorHex
            )
            repository.saveCreditCard(card)
            _uiState.value = AddEditCreditCardUiState.Saved
        } catch (e: Exception) {
            _uiState.value = AddEditCreditCardUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun deleteCreditCard(card: CreditCard) = viewModelScope.launch {
        _uiState.value = AddEditCreditCardUiState.Loading
        try {
            repository.deleteCreditCard(card)
            _uiState.value = AddEditCreditCardUiState.Saved
        } catch (e: Exception) {
            _uiState.value = AddEditCreditCardUiState.Error(e.message ?: "Unknown error")
        }
    }
}
