package model

import kotlinx.serialization.Serializable

@Serializable
data class FlightTracks(
    val timestamp: String,
    val alt: Int,
    val track: Int,
    val gspeed: Int,
    val vspeed: Int,
    val lat: Double,
    val lon: Double,
)

@Serializable
data class FlightTracksModel(
    val fr24_id: String,
    val tracks: List<FlightTracks>
){
    override fun toString(): String {
        return """
        ${fr24_id}
        ${tracks.joinToString(separator = "\n") { it.toString() }}
    """.trimIndent()
    }
}
