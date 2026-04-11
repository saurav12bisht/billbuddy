package com.android.billreminder.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billreminder.data.local.entity.CategoryEntity
import com.android.billreminder.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertCategory(name: String, emoji: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name = name, iconEmoji = emoji, colorHex = colorHex))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (!category.isDefault) {
            viewModelScope.launch {
                // repository.deleteCategory(category.id) // Need to add to repository
            }
        }
    }
}
