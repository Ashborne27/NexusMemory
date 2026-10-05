package com.ashborne.nexusmemory

data class MemoryState(
    val memories: List<MemoryEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Tous",
    val isDarkTheme: Boolean = false,
    val newMemoryText: String = "",
    val newMemoryCategory: String = "Général",
    val editingMemory: MemoryEntity? = null,
    val showStatsDialog: Boolean = false,
    val isAppLocked: Boolean = true
)

sealed class MemoryIntent {
    object AddMemory : MemoryIntent()
    data class DeleteMemory(val memory: MemoryEntity) : MemoryIntent()
    data class TogglePin(val memory: MemoryEntity) : MemoryIntent()
    data class StartEditing(val memory: MemoryEntity) : MemoryIntent()
    object CancelEditing : MemoryIntent()
    data class SaveEditedMemory(val newContent: String, val newCategory: String) : MemoryIntent()
    data class SearchQueryChanged(val query: String) : MemoryIntent()
    data class CategoryFilterSelected(val category: String) : MemoryIntent()
    data class NewMemoryTextChanged(val text: String) : MemoryIntent()
    data class NewMemoryCategorySelected(val category: String) : MemoryIntent()
    object ToggleTheme : MemoryIntent()
    data class ShowStats(val show: Boolean) : MemoryIntent()
    data class ImportJson(val jsonContent: String) : MemoryIntent()
    data class SetAppLocked(val locked: Boolean) : MemoryIntent()
}

sealed class MemoryEffect {
    data class ShowToast(val message: String) : MemoryEffect()
    object ScrollToTop : MemoryEffect()
}
