package util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
        return localDate
    }

    fun isDateWithinRange(
        dateToCheck: String,
        takeoffDate: String?,
        landedDate: String?
    ): Boolean {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val date = LocalDate.parse(dateToCheck, formatter)
        val startDate = takeoffDate?.let { LocalDate.parse(it, formatter) }
        val endDate = landedDate?.let { LocalDate.parse(it, formatter) }

        return (startDate == null || !date.isBefore(startDate)) && (endDate == null || !date.isAfter(endDate))
    }

    fun formatToDateTimeStamp(inputDate: String, isDatetimeTo: Boolean): String {
        val date = LocalDate.parse(inputDate)

        // Format LocalDate to "yyyy-MM-dd'T'HH:mm:ss'Z'" format, using the example format.
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        var formattedDate = date.atStartOfDay().format(formatter) // Keep time at default (start of day)

        if (isDatetimeTo) {
            formattedDate = date.atTime(23, 59, 0).format(formatter)
        }

        // Replace ":" with "%3A" to match the URL-encoded format
        return formattedDate.replace(":", "%3A")
    }


    fun getUTCTime(date: String, timeZone: TimeZone): String {

        // get user timezone from user profile

        // if users timezone offset is 5 hours, also search 5 hours forward if it overlaps into the next day

        // return query for both days of flights for user

        return "w";
    }

}