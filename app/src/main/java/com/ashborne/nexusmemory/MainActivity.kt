package com.ashborne.nexusmemory

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MemoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val titleInput = findViewById<EditText>(R.id.etTitle)
        val contentInput = findViewById<EditText>(R.id.etContent)
        val saveButton = findViewById<Button>(R.id.btnSave)
        val outputText = findViewById<TextView>(R.id.tvOutput)

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
                    if (memories.isEmpty()) {
                        outputText.text = "NexusMemory v2.2\nAucun souvenir enregistré."
                    } else {
                        val sb = StringBuilder("NexusMemory v2.2 - Souvenirs (${memories.size}):\n\n")
                        for (m in memories) {
                            sb.append("• ${m.title}: ${m.content}\n")
                        }
                        outputText.text = sb.toString()
                    }
                }
            }
        }
    }
}
