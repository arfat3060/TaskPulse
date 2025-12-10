package com.example.taskpulse

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class TimesheetGenerator(private val context: Context) {

    fun generateTimesheet(name: String, projectName: String, entries: List<TimesheetEntry>, fileName: String, outputDirectory: Uri?) {
        val workbook: Workbook = XSSFWorkbook()
        val sheet: Sheet = workbook.createSheet("Timesheet")

        // Header Rows
        sheet.createRow(0).createCell(0).setCellValue("Resource Name: $name")
        sheet.createRow(1).createCell(0).setCellValue("Project Name: $projectName")
        sheet.createRow(2).createCell(0).setCellValue("Project Category: MUTUAL_FUNDS")

        // Column Headers
        val dataHeaderRow = sheet.createRow(4)
        val headers = listOf("Sr. No", "Date", "Day", "Intake ID / CR No.", "Work on CR/ Intake ID description details", "Application Name", "Future Benefit", "In Time", "Out Time", "Hours (HH:MM)", "Tasks")
        headers.forEachIndexed { index, header ->
            dataHeaderRow.createCell(index).setCellValue(header)
        }

        // Data Rows
        var rowNum = 5
        var totalMillis: Long = 0
        var totalLeaves = 0
        val weekendCellStyle = createWeekendCellStyle(workbook)

        var i = 0
        while (i < entries.size) {
            val entry = entries[i]
            val dataRow = sheet.createRow(rowNum++)
            dataRow.createCell(0).setCellValue((i + 1).toDouble())
            dataRow.createCell(1).setCellValue(entry.date)
            dataRow.createCell(2).setCellValue(entry.day)
            dataRow.createCell(3).setCellValue(entry.intakeId)

            when {
                entry.isWeekend -> {
                    if (i + 1 < entries.size && entries[i + 1].isWeekend) {
                        val nextRow = sheet.createRow(rowNum++)
                        nextRow.createCell(0).setCellValue((i + 2).toDouble())
                        nextRow.createCell(1).setCellValue(entries[i + 1].date)
                        nextRow.createCell(2).setCellValue(entries[i + 1].day)
                        nextRow.createCell(3).setCellValue(entries[i + 1].intakeId)
                        sheet.addMergedRegion(CellRangeAddress(dataRow.rowNum, nextRow.rowNum, 4, 10))
                        val mergedCell = dataRow.createCell(4)
                        mergedCell.setCellValue("WEEKEND")
                        mergedCell.cellStyle = weekendCellStyle
                        i++ // Increment to skip the next day
                    } else {
                        handleSpecialRow(sheet, dataRow, "WEEKEND", weekendCellStyle)
                    }
                }
                entry.isHoliday -> {
                    handleSpecialRow(sheet, dataRow, entry.holidayName, createHolidayCellStyle(workbook))
                }
                entry.isLeave -> {
                    handleSpecialRow(sheet, dataRow, entry.leaveReason.ifEmpty { "SICK LEAVE" }, createLeaveCellStyle(workbook))
                    totalLeaves++
                }
                else -> {
                    dataRow.createCell(4).setCellValue(entry.workDescription)
                    dataRow.createCell(5).setCellValue(projectName) // Use project name here
                    dataRow.createCell(6).setCellValue(entry.futureBenefit)
                    dataRow.createCell(7).setCellValue(entry.inTime)
                    dataRow.createCell(8).setCellValue(entry.outTime)

                    val hours = calculateHours(entry.inTime, entry.outTime)
                    dataRow.createCell(9).setCellValue(hours.first)
                    dataRow.createCell(10).setCellValue(entry.taskDescription)
                    totalMillis += hours.second
                }
            }
            i++
        }

        // Footer Rows
        val totalRow = sheet.createRow(rowNum + 1)
        totalRow.createCell(8).setCellValue("Total")
        totalRow.createCell(9).setCellValue(formatTotalHours(totalMillis))

        val leavesRow = sheet.createRow(rowNum + 2)
        leavesRow.createCell(8).setCellValue("No of Leaves")
        leavesRow.createCell(9).setCellValue(totalLeaves.toDouble())

        // Save the file
        saveWorkbook(workbook, fileName, outputDirectory)
    }

    private fun handleSpecialRow(sheet: Sheet, row: Row, text: String, style: CellStyle) {
        val mergedCell = row.createCell(4)
        mergedCell.setCellValue(text)
        mergedCell.cellStyle = style
        sheet.addMergedRegion(CellRangeAddress(row.rowNum, row.rowNum, 4, 10))
    }

    private fun createWeekendCellStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.fillForegroundColor = IndexedColors.YELLOW.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        return style
    }

    private fun createHolidayCellStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.fillForegroundColor = IndexedColors.LIGHT_BLUE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        return style
    }

    private fun createLeaveCellStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.fillForegroundColor = IndexedColors.LIGHT_GREEN.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        return style
    }

    private fun saveWorkbook(workbook: Workbook, fileName: String, outputDirectory: Uri?) {
        val contentResolver = context.contentResolver
        val finalFileName = if (fileName.endsWith(".xlsx")) fileName else "$fileName.xlsx"

        val uri: Uri? = if (outputDirectory != null) {
            val directory = DocumentFile.fromTreeUri(context, outputDirectory)
            val existingFile = directory?.findFile(finalFileName)
            existingFile?.delete() // Overwrite existing file
            val file = directory?.createFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", finalFileName)
            file?.uri
        } else {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }
            contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
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