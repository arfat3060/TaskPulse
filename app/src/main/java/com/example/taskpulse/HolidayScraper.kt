package com.example.taskpulse

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale

// Helper data classes for parsing the JSON response from the API
data class HolidayResponse(
    @SerializedName("CBM") val cbm: List<HolidayItem>?,
    @SerializedName("CD") val cd: List<HolidayItem>?,
    @SerializedName("CM") val cm: List<HolidayItem>?,
    @SerializedName("CMOT") val cmot: List<HolidayItem>?,
    @SerializedName("COM") val com: List<HolidayItem>?,
    @SerializedName("FO") val fo: List<HolidayItem>?,
    @SerializedName("IRD") val ird: List<HolidayItem>?,
    @SerializedName("MF") val mf: List<HolidayItem>?,
    @SerializedName("NDM") val ndm: List<HolidayItem>?,
    @SerializedName("NTRP") val ntrp: List<HolidayItem>?,
    @SerializedName("SLBS") val slbs: List<HolidayItem>?
)
data class HolidayItem(val tradingDate: String, val description: String)

class HolidayScraper {

    suspend fun scrapeHolidays(): Map<String, String> = withContext(Dispatchers.IO) {
        val holidays = mutableMapOf<String, String>()
        // The direct API endpoint for trading holidays
        val url = "https://www.nseindia.com/api/holiday-master?type=trading"

        try {
            val jsonResponse = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                .ignoreContentType(true)
                .execute()
                .body()

            val response = Gson().fromJson(jsonResponse, HolidayResponse::class.java)

            val apiDateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
            val appDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)

            val allHolidays = listOfNotNull(
                response.cbm, response.cd, response.cm, response.cmot,
                response.com, response.fo, response.ird, response.mf,
                response.ndm, response.ntrp, response.slbs
            ).flatten()

            allHolidays.forEach { item ->
                try {
                    val date = apiDateFormat.parse(item.tradingDate)
                    if (date != null) {
                        val formattedDate = appDateFormat.format(date)
                        holidays[formattedDate] = item.description
                    }
                } catch (e: java.text.ParseException) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        holidays
    }
}