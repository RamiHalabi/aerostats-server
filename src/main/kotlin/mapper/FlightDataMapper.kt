package mapper

import kotlinx.serialization.Serializable
import model.FlightDataModel

@Serializable
data class FlightDataEntity(
    val flight_id: String,
    val flight_number: String,
    val callsign: String,
    val aircraft: String,
    val registration: String,
    val painted_as: String,
    val operating_as: String,
    val orig_icao: String,
    val dest_icao: String,
    val date_added: String,
    var date: String
)

object FlightDataMapper {
    fun fromModel(model: FlightDataModel): FlightDataEntity {
        return FlightDataEntity(
            flight_id = model.fr24_id,
            flight_number = model.flight,
            callsign = model.callsign,
            aircraft = model.type,
            registration = model.reg,
            painted_as = model.painted_as,
            operating_as = model.operating_as,
            orig_icao = model.orig_icao,
            dest_icao = model.dest_icao,
            date_added = model.timestamp,
            date = model.timestamp
        )
    }

    fun fromEntity(entity: FlightDataEntity): FlightDataModel {
        return FlightDataModel(
            fr24_id = entity.flight_id,
            flight = entity.flight_number,
            callsign = entity.callsign,
            type = entity.aircraft,
            reg = entity.registration,
            painted_as = entity.painted_as,
            operating_as = entity.operating_as,
            orig_icao = entity.orig_icao,
            dest_icao = entity.dest_icao,
            timestamp = entity.date_added
        )
    }
}