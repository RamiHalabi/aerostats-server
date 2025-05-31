package service

import com.ramihalabi.event.Event
import com.ramihalabi.event.EventBus
import model.AirlinesLightModel
import model.FlightDataModel
import model.FlightTracksModel
import repository.FlightDataRepository

class FlightService(
    private val api: FR24API,
    private val repository: FlightDataRepository
) {
    suspend fun getAirlineLight(icao: String): AirlinesLightModel {
        return repository.getAirlineByIcao(icao)
            ?: api.airlinesLight(icao)?.also {
                repository.saveAirline(it)
            }
            ?: AirlinesLightModel(icao = "null", iata = "null", name = "null")
    }

    suspend fun getFlightData(flightNumber: String, authToken: String): FlightDataModel? {
        return repository.getFlightByNumber(flightNumber, authToken)
            ?: api.flightPositionsFull(flightNumber)?.also {
                repository.saveFlight(it, authToken)
                    .onSuccess { result ->
                        println("✅ Saved Flight: ${result.flight}")
                        EventBus.post(Event.FlightSaved(result.flight.fr24_id))
                    }
                    .onFailure { result ->
                        println("❌ Failed to save flight: ${result.message}")
                        EventBus.post(Event.FlightSaveFailed(it, result))
                    }
            }
    }

    suspend fun getFlightTrack(id: String): FlightTracksModel? {
        return repository.getTrack(id)
            ?: api.flightTracks(id)?.also {
                repository.saveTrack(it)
            }
    }
}