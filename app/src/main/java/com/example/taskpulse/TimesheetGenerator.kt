package com.example.taskpulse

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class TimesheetGenerator(private val context: Context) {

    fun generateTimesheet(name: String, projectName: String, entries: List<TimesheetEntry>, fileName: String, outputDirectory: Uri?) {
        val workbook: Workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Timesheet")

        // Create header rows
        val headerRow1 = sheet.createRow(0)
        headerRow1.createCell(0).setCellValue("Resource Name: $name")

        val headerRow2 = sheet.createRow(1)
        headerRow2.createCell(0).setCellValue("Project Name: $projectName")

        val headerRow3 = sheet.createRow(2)
        headerRow3.createCell(0).setCellValue("Project Category: MUTUAL_FUNDS")

        val dataHeaderRow = sheet.createRow(4)
        dataHeaderRow.createCell(0).setCellValue("Sr. No")
        dataHeaderRow.createCell(1).setCellValue("Date")
        dataHeaderRow.createCell(2).setCellValue("Day")
        dataHeaderRow.createCell(3).setCellValue("Intake ID / CR No.")
        dataHeaderRow.createCell(4).setCellValue("Work on CR/ Intake ID description details")
        dataHeaderRow.createCell(5).setCellValue("Application Name")
        dataHeaderRow.createCell(6).setCellValue("Future Benefit")
        dataHeaderRow.createCell(7).setCellValue("In Time")
        dataHeaderRow.createCell(8).setCellValue("Out Time")
        dataHeaderRow.createCell(9).setCellValue("Hours (HH:MM)")
        dataHeaderRow.createCell(10).setCellValue("Tasks")

        var rowNum = 5 // Start data from the 6th row (index 5)
        var totalMillis: Long = 0
        var totalLeaves = 0

        entries.forEachIndexed { index, entry ->
            val dataRow = sheet.createRow(rowNum++)
            dataRow.createCell(0).setCellValue((index + 1).toDouble())
            dataRow.createCell(1).setCellValue(entry.date)
            dataRow.createCell(2).setCellValue(entry.day)

            when {
                entry.isWeekend -> {
                    dataRow.createCell(4).setCellValue("WEEKEND")
                }
                entry.isHoliday -> {
                    dataRow.createCell(4).setCellValue("HOLIDAY")
                }
                entry.isLeave -> {
                    dataRow.createCell(4).setCellValue(entry.leaveReason.ifEmpty { "SICK LEAVE" })
                    totalLeaves++
                }
                else -> {
                    dataRow.createCell(3).setCellValue(entry.intakeId)
                    dataRow.createCell(4).setCellValue(entry.workDescription)
                    dataRow.createCell(5).setCellValue(entry.applicationName)
                    dataRow.createCell(6).setCellValue(entry.futureBenefit)
                    dataRow.createCell(7).setCellValue(entry.inTime)
                    dataRow.createCell(8).setCellValue(entry.outTime)

                    val hours = calculateHours(entry.inTime, entry.outTime)
                    dataRow.createCell(9).setCellValue(hours.first)
                    dataRow.createCell(10).setCellValue(entry.taskDescription)
                    totalMillis += hours.second
                }
            }
        }

        val totalRow = sheet.createRow(rowNum + 1)
        totalRow.createCell(8).setCellValue("Total")
        totalRow.createCell(9).setCellValue(formatTotalHours(totalMillis))

        val leavesRow = sheet.createRow(rowNum + 2)
        leavesRow.createCell(8).setCellValue("No of Leaves")
        leavesRow.createCell(9).setCellValue(totalLeaves.toDouble())

        val contentResolver = context.contentResolver
        val uri: Uri?

        if (outputDirectory != null) {
            // If user selected a custom directory, use it
            val directory = DocumentFile.fromTreeUri(context, outputDirectory)
            val file = directory?.createFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fileName)
            uri = file?.uri
        } else {
            // Otherwise, save to the default Downloads directory using MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.xlsx")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        }

        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateHours(inTime: String, outTime: String): Pair<String, Long> {
        if (inTime.isBlank() || outTime.isBlank()) {
            return "" to 0L
        }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        try {
            val inDate = timeFormat.parse(inTime)
            val outDate = timeFormat.parse(outTime)

            if (inDate != null && outDate != null) {
                val diff = outDate.time - inDate.time
                if (diff >= 0) {
                    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60
                    return String.format(Locale.getDefault(), "%d:%02d", hours, minutes) to diff
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "" to 0L
    }

    private fun formatTotalHours(totalMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(totalMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis) % 60
        return String.format(Locale.getDefault(), "%d:%02d:00", hours, minutes)
    }
}