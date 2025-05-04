package model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class FlightDataResponse(
    val data: List<FlightDataModel>
)

@Serializable
data class FlightDataModel(
    val unique_id: String = UUID.randomUUID().toString(),
    val fr24_id: String,
    val flight: String,
    val callsign: String,
    val timestamp: String,
    val type: String,
    val reg: String,
    val painted_as: String,
    val operating_as: String,
    val orig_icao: String,
    val dest_icao: String,
) {

    override fun toString(): String {
        return """
        ${unique_id}
        Flight ID: $fr24_id
        Flight Number: $flight
        Callsign: $callsign
        Aircraft: $type
        Registration: $reg
        Painted As: $painted_as
        Operating As: $operating_as
        Origin ICAO: $orig_icao
        Destination ICAO: $dest_icao
        Date: $timestamp
    """.trimIndent()
    }
}
