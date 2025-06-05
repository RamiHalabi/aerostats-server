package mapper

import kotlinx.serialization.Serializable
import model.FlightSummaryModel

@Serializable
data class FlightSummaryEntity(
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
    val dest_icao: String,
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
)

object FlightSummaryMapper {
    fun fromModel(model: FlightSummaryModel): FlightSummaryEntity {
        return FlightSummaryEntity(
            fr24_id = model.fr24_id,
            flight = model.flight,
            callsign = model.callsign,
            operating_as = model.operating_as,
            painted_as = model.painted_as,
            type = model.type,
            reg = model.reg,
            orig_icao = model.orig_icao,
            datetime_takeoff = model.datetime_takeoff,
            runway_takeoff = model.runway_takeoff,
            dest_icao = model.destIcao,
            dest_icao_actual = model.dest_icao_actual,
            datetime_landed = model.datetime_landed,
            runway_landed = model.runway_landed,
            flight_time = model.flight_time,
            actual_distance = model.actual_distance,
            circle_distance = model.circle_distance,
            category = model.category,
            first_seen = model.first_seen,
            last_seen = model.last_seen,
            flight_ended = model.flight_ended
        )
    }
}