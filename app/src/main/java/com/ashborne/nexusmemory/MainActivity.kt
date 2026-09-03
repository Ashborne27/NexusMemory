package com.ashborne.nexusmemory

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ashborne.nexusmemory.data.MemoryEntity
import com.ashborne.nexusmemory.viewmodel.MemoryViewModel

class MainActivity : AppCompatActivity() {

    private val memoryViewModel: MemoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etContent = findViewById<EditText>(R.id.etContent)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val listViewMemories = findViewById<ListView>(R.id.listViewMemories)

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isNotEmpty() && content.isNotEmpty()) {
                val memory = MemoryEntity(title = title, content = content)
                memoryViewModel.insert(memory)
                etTitle.text.clear()
                etContent.text.clear()
                Toast.makeText(this, "Mémoire enregistrée avec succès", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            }
        }

        memoryViewModel.allMemories.observe(this) { memories ->
            val displayList = memories.map { "${it.title} : ${it.content}" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
            listViewMemories.adapter = adapter
        }
    }
}
