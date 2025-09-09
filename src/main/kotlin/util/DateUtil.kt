package util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class DateUtil {
    fun formatToDateTimeStamp(inputDate: String, isDatetimeTo: Boolean): String {
        val date = LocalDate.parse(inputDate)
        // TODO: This should be replaced with the user's actual timezone from their profile.
        val userZoneId = ZoneId.of("America/Chicago")

        val localDateTime = if (isDatetimeTo) date.atTime(23, 59, 0) else date.atStartOfDay()
        val utcDateTime = localDateTime.atZone(userZoneId)
            .withZoneSameInstant(ZoneOffset.UTC)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val formattedDate = utcDateTime.format(formatter)

        // Replace ":" with "%3A" to match the URL-encoded format
        return formattedDate.replace(":", "%3A")
    }
}