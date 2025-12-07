package com.example.taskpulse

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val STORAGE_PERMISSION_CODE = 101
    private lateinit var timesheetAdapter: TimesheetAdapter
    private val entries = mutableListOf<TimesheetEntry>()
    private var holidays = mapOf<String, String>()
    private val holidayScraper = HolidayScraper()
    private val gson = Gson()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var calendar: Calendar
    private lateinit var nameEditText: TextInputEditText
    private lateinit var projectNameEditText: TextInputEditText
    private var outputDirectory: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Details"

        sharedPreferences = getSharedPreferences("TaskPulsePrefs", Context.MODE_PRIVATE)
        calendar = Calendar.getInstance()

        nameEditText = findViewById(R.id.name_edit_text)
        projectNameEditText = findViewById(R.id.project_name_edit_text)
        val generateButton = findViewById<FloatingActionButton>(R.id.generate_button)
        val prevMonthButton = findViewById<ImageButton>(R.id.prev_month_button)
        val nextMonthButton = findViewById<ImageButton>(R.id.next_month_button)
        val recyclerView = findViewById<RecyclerView>(R.id.timesheet_recycler_view)

        nameEditText.setText(sharedPreferences.getString("name", ""))
        projectNameEditText.setText(sharedPreferences.getString("projectName", ""))
        val outputUriString = sharedPreferences.getString("outputUri", null)
        if (outputUriString != null) {
            outputDirectory = Uri.parse(outputUriString)
        }

        setupRecyclerView(recyclerView)
        loadHolidays()
        updateMonthYearTextView()
        populateTimesheetEntries()

        prevMonthButton.setOnClickListener {
            saveTimesheetEntries()
            calendar.add(Calendar.MONTH, -1)
            updateMonthYearTextView()
            populateTimesheetEntries()
        }

        nextMonthButton.setOnClickListener {
            saveTimesheetEntries()
            calendar.add(Calendar.MONTH, 1)
            updateMonthYearTextView()
            populateTimesheetEntries()
        }

        generateButton.setOnClickListener {
            showGenerateFileDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveTimesheetEntries()
                saveUserData()
                Toast.makeText(this, "Progress saved", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_refresh_holidays -> {
                refreshHolidays()
                true
            }
            R.id.action_configure_file -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        timesheetAdapter = TimesheetAdapter(entries) { position, is_in_time ->
            showTimePickerDialog(position, is_in_time)
        }
        recyclerView.adapter = timesheetAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun updateMonthYearTextView() {
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        findViewById<TextView>(R.id.month_year_text_view).text = monthYearFormat.format(calendar.time)
    }

    private fun populateTimesheetEntries() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val savedEntriesJson = sharedPreferences.getString("entries_$year-$month", null)

        val existingEntries = if (savedEntriesJson != null) {
            val type = object : TypeToken<MutableList<TimesheetEntry>>() {}.type
            gson.fromJson<MutableList<TimesheetEntry>>(savedEntriesJson, type)
        } else {
            null
        }

        entries.clear()

        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

        for (i in 1..maxDays) {
            tempCalendar.set(Calendar.DAY_OF_MONTH, i)
            val dateString = dateFormat.format(tempCalendar.time)
            val savedEntry = existingEntries?.find { it.date == dateString }

            val dayString = dayFormat.format(tempCalendar.time)
            val dayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
            val holidayName = holidays[dateString]

            if (savedEntry != null) {
                savedEntry.isHoliday = holidayName != null
                savedEntry.holidayName = holidayName ?: ""
                savedEntry.isWeekend = isWeekend
                entries.add(savedEntry)
            } else {
                entries.add(TimesheetEntry(
                    date = dateString,
                    day = dayString,
                    isWeekend = isWeekend,
                    isHoliday = holidayName != null,
                    holidayName = holidayName ?: ""
                ))
            }
        }
        timesheetAdapter.notifyDataSetChanged()
    }


    private fun saveTimesheetEntries() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val entriesJson = gson.toJson(entries)
        sharedPreferences.edit().putString("entries_$year-$month", entriesJson).apply()
    }

    private fun saveUserData() {
        sharedPreferences.edit()
            .putString("name", nameEditText.text.toString())
            .putString("projectName", projectNameEditText.text.toString())
            .apply()
    }

    private fun loadHolidays() {
        val holidaysJson = sharedPreferences.getString("holidays", null)
        if (holidaysJson != null) {
            val type = object : TypeToken<Map<String, String>>() {}.type
            holidays = gson.fromJson(holidaysJson, type)
        } else {
            holidays = mapOf(
                "01-01-2025" to "New Year's Day",
                "26-01-2025" to "Republic Day",
                "29-03-2025" to "Good Friday",
                "15-08-2025" to "Independence Day",
                "02-10-2025" to "Gandhi Jayanti",
                "25-12-2025" to "Christmas"
            )
        }
    }

    private fun refreshHolidays() {
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Refreshing holidays...", Toast.LENGTH_SHORT).show()
            val scrapedHolidays = holidayScraper.scrapeHolidays()
            if (scrapedHolidays.isNotEmpty()) {
                holidays = scrapedHolidays
                val holidaysJson = gson.toJson(holidays)
                sharedPreferences.edit().putString("holidays", holidaysJson).apply()
                populateTimesheetEntries()
                Toast.makeText(this@MainActivity, "Holidays refreshed successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to refresh holidays", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTimePickerDialog(position: Int, is_in_time: Boolean) {
        val cal = Calendar.getInstance()
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
            if (is_in_time) {
                entries[position].inTime = time
            } else {
                entries[position].outTime = time
            }
            timesheetAdapter.notifyItemChanged(position)
        }
        TimePickerDialog(this, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun showGenerateFileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_generate_file, null)
        val fileNameEditText = dialogView.findViewById<EditText>(R.id.file_name_edit_text)
        val selectedPathTextView = dialogView.findViewById<TextView>(R.id.selected_path_text_view)
        val monthYearFormat = SimpleDateFormat("MMMM_yyyy", Locale.getDefault())
        val defaultFileName = "Timesheet_${monthYearFormat.format(calendar.time)}"
        fileNameEditText.setText(defaultFileName)
        selectedPathTextView.text = "Selected Path: ${outputDirectory?.path ?: "Default (Downloads)"}"


        val browseButton = dialogView.findViewById<Button>(R.id.browse_button)
        browseButton.setOnClickListener {
            openDirectoryPicker()
        }

        AlertDialog.Builder(this)
            .setTitle("Generate Timesheet")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val fileName = fileNameEditText.text.toString()
                if (fileName.isNotEmpty()) {
                    val name = nameEditText.text.toString()
                    val projectName = projectNameEditText.text.toString()
                    val timesheetGenerator = TimesheetGenerator(this)
                    timesheetGenerator.generateTimesheet(name, projectName, entries, fileName, outputDirectory)
                    Toast.makeText(this, "Timesheet saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "File name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        resultLauncher.launch(intent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also {
                outputDirectory = it
                Toast.makeText(this, "Output path selected: ${it.path}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveTimesheetEntries()
        saveUserData()
    }
}