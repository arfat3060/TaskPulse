package com.example.taskpulse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fileNameEditText: TextInputEditText
    private var outputDirectory: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences("TaskPulsePrefs", Context.MODE_PRIVATE)

        fileNameEditText = findViewById(R.id.file_name_edit_text)
        fileNameEditText.setText(sharedPreferences.getString("fileName", "Timesheet"))

        val browseButton = findViewById<Button>(R.id.browse_button)
        browseButton.setOnClickListener {
            openDirectoryPicker()
        }

        val saveButton = findViewById<Button>(R.id.save_settings_button)
        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        resultLauncher.launch(intent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also {
                outputDirectory = it
                Toast.makeText(this, "Output path selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveSettings() {
        val editor = sharedPreferences.edit()
        editor.putString("fileName", fileNameEditText.text.toString())
        outputDirectory?.let {
            editor.putString("outputUri", it.toString())
        }
        editor.apply()
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}