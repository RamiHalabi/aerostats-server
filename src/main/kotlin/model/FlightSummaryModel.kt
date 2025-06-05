package model

import kotlinx.serialization.Serializable

@Serializable
data class FlightSummaryResponse(
    val data: List<FlightSummaryModel>
)

@Serializable
data class FlightSummaryModel(
    val fr24_id: String,
    val flight: String,
    val callsign: String,
    val operating_as: String,
    val painted_as: String,
    val type: String,
    val reg: String,
    val orig_icao: String,
    val datetime_takeoff: String,
    val runway_takeoff: String,
    val destIcao: String,
    val dest_icao_actual: String,
    val datetime_landed: String?,
    val runway_landed: String?,
    val flight_time: String?,
    val actual_distance: Double,
    val circle_distance: Double,
    val category: String,
    val first_seen: String,
    val last_seen: String,
    val flight_ended: Boolean
): FlightData() {
    override fun toString(): String {
        return """
        Flight ID: $fr24_id
        Flight Number: $flight
        Callsign: $callsign
        Operating As: $operating_as
        Painted As: $painted_as
        Type: $type
        Registration: $reg
        Origin ICAO: $orig_icao
        Date/Time Takeoff: $datetime_takeoff
        Runway Takeoff: $runway_takeoff
        Destination ICAO: $destIcao
        Actual Destination ICAO: $dest_icao_actual
        Date/Time Landed: $datetime_landed
        Runway Landed: $runway_landed
        Flight Time: $flight_time
        Actual Distance: $actual_distance
        Circle Distance: $circle_distance
        Category: $category
        First Seen: $first_seen
        Last Seen: $last_seen
        Flight Ended: $flight_ended
        """.trimIndent()
    }
}



