package service

import event.Event
import event.EventBus
import controller.FlightDataRequest
import model.*
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

    suspend fun getFlightData(flight: FlightDataRequest, authToken: String): FlightDataModel? {
        return repository.getFlight(flight)
            ?: api.flightPositionsFull(flight)?.also {
                repository.saveFlight(it, authToken)
                    .onSuccess { result ->
                        println("✅ Saved Flight Data: ${result.flight}")
                        EventBus.post(Event.FlightSaved(result.flight.toString()))
                    }
                    .onFailure { result ->
                        println("❌ Failed to save flight data: ${result.message}")
                        EventBus.post(Event.FlightSaveFailed(it, result))
                    }
            }
    }

    suspend fun getFlightSummary(flightId: String, authToken: String): FlightSummaryModel? {
        return repository.getFlightSummary(flightId) ?: api.flightSummaryFull(flightId)?. also {
            repository.saveFlightSummary(it, authToken)
                .onSuccess { result ->
                    println("✅ Saved Flight Summary: ${result.flight}")
                    EventBus.post(Event.FlightSaved(result.flight.toString()))
                }
                .onFailure { result ->
                    println("❌ Failed to save flight summary: ${result.message}")
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