package com.android.billreminder.ui.billform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.repository.BillRepository
import com.android.billreminder.domain.model.Bill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AddEditBillUiState {
    data object Idle : AddEditBillUiState
    data object Saving : AddEditBillUiState
    data class Saved(val billId: Long) : AddEditBillUiState
    data class Error(val message: String) : AddEditBillUiState
}

@HiltViewModel
class AddEditBillViewModel @Inject constructor(
    private val billRepository: BillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddEditBillUiState>(AddEditBillUiState.Idle)
    val uiState: StateFlow<AddEditBillUiState> = _uiState.asStateFlow()

    suspend fun loadBill(billId: Long): Bill? = billRepository.getBillById(billId)

    fun saveBill(bill: Bill, isEdit: Boolean) {
        viewModelScope.launch {
            _uiState.value = AddEditBillUiState.Saving
            runCatching {
                if (isEdit) {
                    billRepository.updateBill(bill)
                    bill.id
                } else {
                    billRepository.saveBill(bill)
                }
            }.onSuccess { billId ->
                _uiState.value = AddEditBillUiState.Saved(billId)
            }.onFailure { error ->
                _uiState.value = AddEditBillUiState.Error(error.message ?: "Unable to save bill")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddEditBillUiState.Idle
    }
}
