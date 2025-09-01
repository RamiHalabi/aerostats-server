package service

import io.ktor.util.logging.*
import mapper.FlightSummaryMapper
import model.*
import plugin.getUserIdFromContext
import repository.FlightDataRepository
import util.FlightRequest

interface FlightServiceInterface {
    suspend fun getAirlineLight(icao: FlightRequest.Airline): AirlinesLightModel?
    suspend fun getFlightSummary(flight: FlightRequest.Summary): List<FlightSummaryModel>?
    suspend fun getAllFlights(): List<FlightSummaryModel>
    suspend fun getFlightTrack(flight: FlightRequest.Track): FlightTracksModel?
    suspend fun getAllFlightTracks(): List<FlightTracksModel?>
    suspend fun saveFlights(request: FlightRequest.Save): List<FlightSummaryModel>?
}


class FlightService(
    private val repository: FlightDataRepository,
    private val flightSummaryMapper: FlightSummaryMapper,
    private val logger: Logger
) : FlightServiceInterface {
    override suspend fun getAirlineLight(icao: FlightRequest.Airline): AirlinesLightModel? {
        val result = repository.getAirline(icao)
        return when {
            result.isSuccess -> {
                result.getOrNull()
            }
            result.isFailure -> {
                handleFlightSummaryError(result, "Get Airline", icao.icao)
                null
            }
            else -> null
        }

    }

    override suspend fun getFlightSummary(flight: FlightRequest.Summary): List<FlightSummaryModel>? {
        val result = repository.getFlightSummary(flight)

        return if (result.isSuccess) {
            result.getOrNull()?.let { flightSummaryMapper.fromEntityList(it) }
        } else {
            handleFlightSummaryError(result, "Retrieve Flights Summary", flight.callsign)
            null
        }
    }

    override suspend fun getAllFlights(): List<FlightSummaryModel> {
        val userId = getUserIdFromContext()

        return repository.getAllFlights()
            .fold(
                onSuccess = { entities ->
                    entities?.map { flightSummaryMapper.fromEntity(it) } ?: emptyList()
                },
                onFailure = { error ->
                    logger.error("Failed to retrieve flights for user $userId", error)
                    emptyList()
                }
            )
    }

    override suspend fun getFlightTrack(flight: FlightRequest.Track): FlightTracksModel? {
        val result = repository.getTrack(flight)

        return if (result.isSuccess) {
            val trackData = result.getOrNull()
            if (trackData != null) {
                logger.info("Successfully retrieved track data for flight ID '${flight.flightId}'")
                trackData
            } else {
                logger.warn("No track data found for flight ID '${flight.flightId}'")
                null
            }
        } else {
            handleFlightSummaryError(result, "Retrieve Flight Track", flight.flightId)
            null
        }
    }
    
     override suspend fun getAllFlightTracks(): List<FlightTracksModel?> {
         val flightIds = repository.getAllFlightIds()

        return repository.getAllTracks(flightIds)
            .fold(
                onSuccess = { entities ->
                    entities
                },
                onFailure = { error ->
                    logger.error("Failed to retrieve flights for user ${getUserIdFromContext()}", error)
                    emptyList()
                }
            )
    }

    override suspend fun saveFlights(request: FlightRequest.Save): List<FlightSummaryModel>? {

        val userId = getUserIdFromContext()
        // Step 1: Save flight summaries
        val flightSummaryEntities = flightSummaryMapper.fromModelList(request.flights)
        val flightSummaryResult = repository.saveFlightSummaries(flightSummaryEntities)

        // Step 2: Save flight tracks
        val flightIds = flightSummaryEntities.map { it.fr24_id }
        val trackRequests = flightIds.filterNotNull().filter { it.isNotEmpty() }.map { FlightRequest.Track(it) }
        val tracks = trackRequests.mapNotNull { getFlightTrack(it) }
        val tracksSaveResult = repository.saveTracks(tracks)

        // Step 3: Link flights to user
        val userFlightResult = flightSummaryEntities.mapNotNull { it.fr24_id }.filter { it.isNotEmpty() }
            .let { repository.saveUserFlights(it) }



        return flightSummaryResult.fold(
            onSuccess = { entities ->
                entities.let { flightSummaryMapper.fromEntityList(it.flights) }
            },
            onFailure = { error ->
                logger.error("Failed to save flights for user $userId", error)
                null
            }
        )
    }


//    private fun processResults(results) {
//
//        // final function that returns the results packaged neatly with there responses.
//    }



    /**
     * Handles errors related to flight summary operations by logging a failure message.
     *
     * @param result The result object containing success or exception details.
     * @param operation The operation being performed when the error occurred (e.g., "Retrieve Flights Summary").
     * @param flightIdentifier The identifier of the flight related to the operation.
     */
    private fun handleFlightSummaryError(
        result: Result<*>,
        operation: String,
        flightIdentifier: String
    ) {
        val error = result.exceptionOrNull()
        logger.info("❌ Failed to $operation for flight '${flightIdentifier}': ${error?.message}")
    }

}
