package service

import com.ramihalabi.event.Event
import com.ramihalabi.event.EventBus
import model.AirlinesLightModel
import model.FlightDataModel
import model.FlightTracksModel
import repository.FlightDataRepository

import model.*

class FlightService(
    private val api: FR24API,
    private val repository: FlightDataRepository
) {
    suspend fun getAirlineLight(icao: String): AirlinesLightModel {
        return repository.getAirlineByIcao(icao)
            ?: api.airlinesLight(icao)?.also {
                repository.saveAirline(it)
                EventBus.post(Event.AirlineSaved(it))
            }
            ?: AirlinesLightModel(icao = "null", iata = "null", name = "null")
    }

    suspend fun getFlightData(flightNumber: String): FlightDataModel? {
        return repository.getFlightByNumber(flightNumber)
            ?: api.flightPositionsFull(flightNumber)?.also {
                repository.saveFlight(it)
                EventBus.post(Event.FlightSaved(it))
            }
    }

    suspend fun getFlightTrack(id: String): FlightTracksModel? {
        return repository.getTrack(id)
            ?: api.flightTracks(id)?.also {
                repository.saveTrack(it)
                EventBus.post(Event.TrackSaved(it))
            }
    }
}