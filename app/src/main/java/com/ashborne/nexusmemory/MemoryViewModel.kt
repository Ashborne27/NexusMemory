package com.ashborne.nexusmemory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModel(private val dao: MemoryDao) : ViewModel() {

    val categories = listOf("Général", "Pro", "Personnel", "Idée", "Urgent")

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("Tous")
    private val _isDarkTheme = MutableStateFlow(false)
    private val _newMemoryText = MutableStateFlow("")
    private val _newMemoryCategory = MutableStateFlow("Général")
    private val _editingMemory = MutableStateFlow<MemoryEntity?>(null)
    private val _showStatsDialog = MutableStateFlow(false)
    private val _isAppLocked = MutableStateFlow(true)

    private val _effect = MutableSharedFlow<MemoryEffect>()
    val effect: SharedFlow<MemoryEffect> = _effect.asSharedFlow()

    private val memoriesFlow = combine(_searchQuery, _selectedCategory) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        dao.getFilteredMemories(
            category = if (category == "Tous") null else category,
            query = query.ifBlank { null }
        )
    }

    private val formStateFlow = combine(
        _newMemoryText,
        _newMemoryCategory,
        _editingMemory,
        _showStatsDialog,
        _isAppLocked
    ) { newText, newCat, editing, stats, locked ->
        Quintuple(newText, newCat, editing, stats, locked)
    }

    private val uiSettingsFlow = combine(
        _searchQuery,
        _selectedCategory,
        _isDarkTheme
    ) { search, cat, dark ->
        Triple(search, cat, dark)
    }

    val state: StateFlow<MemoryState> = combine(
        memoriesFlow,
        uiSettingsFlow,
        formStateFlow
    ) { memories, (search, cat, dark), (newText, newCat, editing, stats, locked) ->
        MemoryState(
            memories = memories,
            searchQuery = search,
            selectedCategory = cat,
            isDarkTheme = dark,
            newMemoryText = newText,
            newMemoryCategory = newCat,
            editingMemory = editing,
            showStatsDialog = stats,
            isAppLocked = locked
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MemoryState())

    fun processIntent(intent: MemoryIntent) {
        viewModelScope.launch {
            when (intent) {
                is MemoryIntent.AddMemory -> {
                    val text = _newMemoryText.value.trim()
                    if (text.isNotEmpty()) {
                        dao.insertMemory(
                            MemoryEntity(
                                content = text,
                                category = _newMemoryCategory.value
                            )
                        )
                        _newMemoryText.value = ""
                        _effect.emit(MemoryEffect.ShowToast("Pensée enregistrée"))
                    }
                }
                is MemoryIntent.DeleteMemory -> {
                    dao.deleteMemory(intent.memory)
                    _effect.emit(MemoryEffect.ShowToast("Pensée supprimée"))
                }
                is MemoryIntent.TogglePin -> {
                    dao.updateMemory(intent.memory.copy(isPinned = !intent.memory.isPinned))
                }
                is MemoryIntent.StartEditing -> _editingMemory.value = intent.memory
                is MemoryIntent.CancelEditing -> _editingMemory.value = null
                is MemoryIntent.SaveEditedMemory -> {
                    _editingMemory.value?.let { memory ->
                        dao.updateMemory(memory.copy(content = intent.newContent, category = intent.newCategory))
                        _editingMemory.value = null
                        _effect.emit(MemoryEffect.ShowToast("Pensée modifiée"))
                    }
                }
                is MemoryIntent.SearchQueryChanged -> _searchQuery.value = intent.query
                is MemoryIntent.CategoryFilterSelected -> _selectedCategory.value = intent.category
                is MemoryIntent.NewMemoryTextChanged -> _newMemoryText.value = intent.text
                is MemoryIntent.NewMemoryCategorySelected -> _newMemoryCategory.value = intent.category
                is MemoryIntent.ToggleTheme -> _isDarkTheme.value = !_isDarkTheme.value
                is MemoryIntent.ShowStats -> _showStatsDialog.value = intent.show
                is MemoryIntent.SetAppLocked -> _isAppLocked.value = intent.locked
                is MemoryIntent.ImportJson -> {
                    try {
                        val jsonArray = JSONArray(intent.jsonContent)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            dao.insertMemory(
                                MemoryEntity(
                                    content = obj.getString("content"),
                                    category = obj.getString("category"),
                                    timestamp = obj.getLong("timestamp"),
                                    isPinned = obj.getBoolean("isPinned")
                                )
                            )
                        }
                        _effect.emit(MemoryEffect.ShowToast("Importation réussie"))
                    } catch (e: Exception) {
                        _effect.emit(MemoryEffect.ShowToast("Erreur d'importation : Format non reconnu"))
                    }
                }
            }
        }
    }

    suspend fun getExportData(passphrase: String): String {
        val memories = state.value.memories
        val jsonArray = JSONArray()
        memories.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("content", m.content)
                put("category", m.category)
                put("timestamp", m.timestamp)
                put("isPinned", m.isPinned)
            }
            jsonArray.put(obj)
        }
        val rawJson = jsonArray.toString()
        return if (passphrase.isBlank()) rawJson else CryptoManager.encrypt(rawJson, passphrase.toCharArray())
    }

    fun importData(rawOrEncrypted: String, passphrase: String) {
        viewModelScope.launch {
            try {
                val jsonStr = if (passphrase.isBlank()) {
                    rawOrEncrypted
                } else {
                    CryptoManager.decrypt(rawOrEncrypted, passphrase.toCharArray())
                }
                processIntent(MemoryIntent.ImportJson(jsonStr))
            } catch (e: Exception) {
                _effect.emit(MemoryEffect.ShowToast("Clé/Mot de passe incorrect ou fichier altéré"))
            }
        }
    }

    class Factory(private val dao: MemoryDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MemoryViewModel(dao) as T
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
