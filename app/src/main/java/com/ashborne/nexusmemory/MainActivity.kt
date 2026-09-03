package com.ashborne.nexusmemory

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MemoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val titleInput = findViewById<EditText>(R.id.etTitle)
        val contentInput = findViewById<EditText>(R.id.etContent)
        val saveButton = findViewById<Button>(R.id.btnSave)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = MemoryAdapter { memory ->
            viewModel.deleteMemory(memory)
        }
        recyclerView.adapter = adapter

        saveButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val content = contentInput.text.toString().trim()
            if (title.isNotEmpty() && content.isNotEmpty()) {
                viewModel.addMemory(title, content)
                titleInput.text.clear()
                contentInput.text.clear()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allMemories.collect { memories ->
                    adapter.submitList(memories)
                }
            }
        }
    }
}
