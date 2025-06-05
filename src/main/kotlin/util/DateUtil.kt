package util

import java.text.SimpleDateFormat
import java.util.*

class DateUtil {

    fun getLocalDate(date: String): String {
        val utcParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        utcParser.timeZone = TimeZone.getTimeZone("UTC")
        val utcDate = utcParser.parse(date)
        val userTimeZone = TimeZone.getTimeZone("America/Chicago")
        val localFormatter = SimpleDateFormat("yyyy-MM-dd")
        localFormatter.timeZone = userTimeZone
        val localDate = localFormatter.format(utcDate)
        println("Local date result: $localDate")
        return localDate
    }
}