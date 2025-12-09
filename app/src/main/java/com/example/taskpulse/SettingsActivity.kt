package com.example.taskpulse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fileNameEditText: TextInputEditText
    private lateinit var selectedPathTextView: TextView
    private var outputDirectory: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences("TaskPulsePrefs", Context.MODE_PRIVATE)

        fileNameEditText = findViewById(R.id.file_name_edit_text)
        selectedPathTextView = findViewById(R.id.selected_path_text_view)

        loadSettings()

        val chooseFolderButton = findViewById<Button>(R.id.choose_folder_button)
        chooseFolderButton.setOnClickListener {
            openDirectoryPicker()
        }

        val saveButton = findViewById<Button>(R.id.save_settings_button)
        saveButton.setOnClickListener {
            saveSettings()
        }

        val resetButton = findViewById<Button>(R.id.reset_settings_button)
        resetButton.setOnClickListener {
            resetSettings()
        }
    }

    private fun loadSettings() {
        val monthYearFormat = SimpleDateFormat("MMMM_yyyy", Locale.getDefault())
        val defaultFileName = "Timesheet_${monthYearFormat.format(Calendar.getInstance().time)}"
        val savedFileName = sharedPreferences.getString("fileName", defaultFileName)
        fileNameEditText.setText(savedFileName)

        val outputUriString = sharedPreferences.getString("outputUri", null)
        if (outputUriString != null) {
            outputDirectory = Uri.parse(outputUriString)
            selectedPathTextView.text = "Selected: ${getFriendlyPath(outputDirectory)}"
        } else {
            selectedPathTextView.text = "Selected: Downloads"
        }
    }

    private fun resetSettings() {
        sharedPreferences.edit().clear().apply()
        loadSettings()
        Toast.makeText(this, "Settings reset to default", Toast.LENGTH_SHORT).show()
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        resultLauncher.launch(intent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also {
                outputDirectory = it
                selectedPathTextView.text = "Selected: ${getFriendlyPath(it)}"
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

    private fun getFriendlyPath(uri: Uri?): String {
        if (uri == null) return "Downloads"
        if (DocumentsContract.isTreeUri(uri)) {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            if (split.size > 1) {
                return split[1].replace("/", " > ")
            }
        }
        return uri.path?.substringAfterLast(":")?.replace("/", " > ") ?: "Default (Downloads)"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}