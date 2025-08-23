package mapper

import entity.FlightSummaryEntity
import model.FlightSummaryModel

class FlightSummaryMapper {
    fun fromModel(model: FlightSummaryModel): FlightSummaryEntity {
        return FlightSummaryEntity(
            fr24_id = model.fr24Id,
            flight = model.flight,
            callsign = model.callsign,
            operating_as = model.operatingAs,
            painted_as = model.paintedAs,
            type = model.type,
            reg = model.reg,
            orig_icao = model.origIcao,
            datetime_takeoff = model.datetimeTakeoff,
            runway_takeoff = model.runwayTakeoff,
            dest_icao = model.destIcao,
            dest_icao_actual = model.destIcaoActual,
            datetime_landed = model.datetimeLanded,
            runway_landed = model.runwayLanded,
            flight_time = model.flightTime,
            actual_distance = model.actualDistance,
            circle_distance = model.circleDistance,
            category = model.category,
            first_seen = model.firstSeen,
            last_seen = model.lastSeen,
            flight_ended = model.flightEnded
        )
    }

    fun fromEntity(entity: FlightSummaryEntity): FlightSummaryModel {
        return FlightSummaryModel(
            fr24Id = entity.fr24_id,
            flight = entity.flight,
            callsign = entity.callsign,
            operatingAs = entity.operating_as,
            paintedAs = entity.painted_as,
            type = entity.type,
            reg = entity.reg,
            origIcao = entity.orig_icao,
            datetimeTakeoff = entity.datetime_takeoff,
            runwayTakeoff = entity.runway_takeoff,
            destIcao = entity.dest_icao,
            destIcaoActual = entity.dest_icao_actual,
            datetimeLanded = entity.datetime_landed,
            runwayLanded = entity.runway_landed,
            flightTime = entity.flight_time,
            actualDistance = entity.actual_distance,
            circleDistance = entity.circle_distance,
            category = entity.category,
            firstSeen = entity.first_seen,
            lastSeen = entity.last_seen,
            flightEnded = entity.flight_ended
        )
    }

    fun fromModelList(models: List<FlightSummaryModel>): List<FlightSummaryEntity> {
        return models.map { fromModel(it) }
    }

    fun fromEntityList(entities: List<FlightSummaryEntity>): List<FlightSummaryModel> {
        return entities.map { fromEntity(it) }
    }
}
