package model

import kotlinx.serialization.Serializable

@Serializable
data class FlightSummaryModel(
    val fr24Id: String?,
    val flight: String?,
    val callsign: String?,
    val operatingAs: String?,
    val paintedAs: String?,
    val type: String?,
    val reg: String?,
    val origIcao: String?,
    val datetimeTakeoff: String?,
    val runwayTakeoff: String?,
    val destIcao: String?,
    val destIcaoActual: String?,
    val datetimeLanded: String?,
    val runwayLanded: String?,
    val flightTime: String?,
    val actualDistance: Double?,
    val circleDistance: Double?,
    val category: String?,
    val firstSeen: String?,
    val lastSeen: String?,
    val flightEnded: Boolean?
) {

}



