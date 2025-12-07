package com.example.taskpulse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class HolidayScraper {

    suspend fun scrapeHolidays(url: String): List<String> = withContext(Dispatchers.IO) {
        val holidays = mutableListOf<String>()
        try {
            val doc = Jsoup.connect(url).get()
            val table = doc.getElementById("table-holiday")
            val rows = table?.select("tr")
            val dateFormat = java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.ENGLISH)
            val outputFormat = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.ENGLISH)

            rows?.forEach { row ->
                val cells = row.select("td")
                if (cells.size > 1) {
                    val dateString = cells[1].text()
                    try {
                        val date = dateFormat.parse(dateString)
                        if (date != null) {
                            holidays.add(outputFormat.format(date))
                        }
                    } catch (e: java.text.ParseException) {
                        // Ignore rows that don't have a valid date
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        holidays
    }
}