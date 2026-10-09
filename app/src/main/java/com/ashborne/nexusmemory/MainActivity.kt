package com.ashborne.nexusmemory

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.ashborne.nexusmemory.data.AppDatabase
import com.ashborne.nexusmemory.data.MemoryEntity
import com.ashborne.nexusmemory.ui.security.BiometricAuthManager
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricAuthManager = BiometricAuthManager(this)
        val database = AppDatabase.getDatabase(this)
        val dao = database.memoryDao()

        setContent {
            MaterialTheme {
                val memoriesList by dao.getAllMemories().collectAsState(initial = emptyList())
                NexusApp(
                    biometricAuthManager = biometricAuthManager,
                    memories = memoriesList,
                    onAddThought = { text, category ->
                        lifecycleScope.launch {
                            dao.insertMemory(MemoryEntity(text = text, category = category))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp(
    biometricAuthManager: BiometricAuthManager,
    memories: List<MemoryEntity>,
    onAddThought: (String, String) -> Unit
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var newThoughtText by remember { mutableStateOf("") }

    val categories = listOf("Tous", "Général", "Pro", "Personnel")

    val filteredMemories = memories.filter { item ->
        val matchesCategory = (selectedCategory == "Tous" || item.category == selectedCategory)
        val matchesQuery = item.text.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NexusMemory v6.4") },
                actions = {
                    IconButton(onClick = {
                        if (isUnlocked) {
                            isUnlocked = false
                        } else {
                            if (biometricAuthManager.canAuthenticate()) {
                                biometricAuthManager.authenticate(
                                    onSuccess = { isUnlocked = true },
                                    onError = { }
                                )
                            } else {
                                isUnlocked = true
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.Lock else Icons.Default.Lock,
                            contentDescription = "Verrou biométrique"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Rechercher une pensée...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = (selectedCategory == category),
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newThoughtText,
                    onValueChange = { newThoughtText = it },
                    label = { Text("Nouvelle pensée") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (newThoughtText.isNotBlank()) {
                        val categoryToAssign = if (selectedCategory == "Tous") "Général" else selectedCategory
                        onAddThought(newThoughtText, categoryToAssign)
                        newThoughtText = ""
                    }
                }) {
                    Text("Ajouter")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isUnlocked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Coffre verrouillé. Appuyez sur le cadenas pour l'authentification biométrique.")
                }
            } else if (filteredMemories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucune mémoire enregistrée.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMemories) { memory ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = memory.text, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Catégorie : ${memory.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
