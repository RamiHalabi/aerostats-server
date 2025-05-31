package mapper

import kotlinx.serialization.Serializable
import model.FlightDataModel

@Serializable
data class FlightData(
    val flight_id: String,
    val flight_number: String,
    val callsign: String,
    val aircraft: String,
    val registration: String,
    val painted_as: String,
    val operating_as: String,
    val orig_icao: String,
    val dest_icao: String,
    val date_added: String
)

object FlightDataMapper {
    fun fromModel(model: FlightDataModel): FlightData {
        return FlightData(
            flight_id = model.fr24_id,
            flight_number = model.flight,
            callsign = model.callsign,
            aircraft = model.type,
            registration = model.reg,
            painted_as = model.painted_as,
            operating_as = model.operating_as,
            orig_icao = model.orig_icao,
            dest_icao = model.dest_icao,
            date_added = model.timestamp // Ensure consistent format, e.g. ISO 8601 if needed
        )
    }
}