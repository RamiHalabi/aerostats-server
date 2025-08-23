package entity

import kotlinx.serialization.Serializable

@Serializable
data class FlightSummaryEntity(
    val fr24_id: String?,
    val flight: String?,
    val callsign: String?,
    val operating_as: String?,
    val painted_as: String?,
    val type: String?,
    val reg: String?,
    val orig_icao: String?,
    val datetime_takeoff: String?,
    val runway_takeoff: String?,
    val dest_icao: String?,
    val dest_icao_actual: String?,
    val datetime_landed: String?,
    val runway_landed: String?,
    val flight_time: String?,
    val actual_distance: Double?,
    val circle_distance: Double?,
    val category: String?,
    val first_seen: String?,
    val last_seen: String?,
    val flight_ended: Boolean?
)