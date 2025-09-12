package service

import io.ktor.util.logging.*
import mapper.FlightSummaryMapper
import model.*
import plugin.getUserIdFromContext
import repository.FlightDataRepository
import util.DateUtil
import util.FlightRequest

interface FlightServiceInterface {
    suspend fun getAirlineLight(icao: FlightRequest.Airline): AirlinesLightModel?
    suspend fun getFlightSummary(flight: FlightRequest.Summary): List<FlightSummaryModel>?
    suspend fun getAllFlights(): List<FlightSummaryModel>?
    suspend fun getFlightTrack(flight: FlightRequest.Track): FlightTracksModel?
    suspend fun getAllFlightTracks(): List<FlightTracksModel?>?
    suspend fun saveFlights(request: FlightRequest.Save): List<FlightSummaryModel>?
}


class FlightService(
    private val repository: FlightDataRepository,
    private val flightSummaryMapper: FlightSummaryMapper,
    private val dateUtil: DateUtil,
    private val logger: Logger
) : FlightServiceInterface {
    override suspend fun getAirlineLight(icao: FlightRequest.Airline): AirlinesLightModel? {
        val result = repository.getAirline(icao)
        return if(result.isSuccess) {
                result.getOrNull()
        } else {
            result.exceptionOrNull()?.let {
                handleFlightSummaryError(
                    getUserIdFromContext(),
                    it,
                    "getAirlineLight for ${icao}"
                )
            }
            null
        }
    }

    override suspend fun getFlightSummary(flight: FlightRequest.Summary): List<FlightSummaryModel>? {

        // first format date from user Locale to UTC then get flight from repository
        val formattedFlight = flight.copy(
            datetimeFrom = dateUtil.formatToDateTimeStamp(flight.datetimeFrom, false),
            datetimeTo = dateUtil.formatToDateTimeStamp(flight.datetimeTo, true)
        )
        val result = repository.getFlightSummary(formattedFlight)

        return if (result.isSuccess) {
            result.getOrNull()?.let { flightSummaryMapper.fromEntityList(it) }
        } else {
            result.exceptionOrNull()?.let {
                handleFlightSummaryError(
                    getUserIdFromContext(),
                    it,
                    "getFlightSummary for ${flight.callsign}"
                )
            }
            null
        }
    }

    override suspend fun getAllFlights(): List<FlightSummaryModel>? {
        return repository.getAllFlights()
            .fold(
                onSuccess = { entities ->
                    entities?.map { flightSummaryMapper.fromEntity(it) } ?: emptyList()
                },
                onFailure = { error ->
                    handleFlightSummaryError(
                        getUserIdFromContext(),
                        error,
                        "getAllFlights"
                    )
                    null
                }
            )
    }

    override suspend fun getFlightTrack(flight: FlightRequest.Track): FlightTracksModel? {
        val result = repository.getTrack(flight)

        return if (result.isSuccess) {
            result.getOrNull()
        } else {
            result.exceptionOrNull()?.let {
                handleFlightSummaryError(
                    getUserIdFromContext(),
                    it,
                    "getFlightTrack for ${flight.flightId}"
                )
            }
            null
        }
    }

    override suspend fun getAllFlightTracks(): List<FlightTracksModel?>? {
        return repository.getAllFlightIds().fold(
            onSuccess = { flightIds ->
                if (flightIds.isNullOrEmpty()) {
                    emptyList()
                } else {
                    repository.getAllTracks(flightIds).fold(
                        onSuccess = { tracks -> tracks },
                        onFailure = { error ->
                            handleFlightSummaryError(
                                getUserIdFromContext(),
                                error,
                                "getAllFlightTracks"
                            )
                            null
                        }
                    )
                }
            },
            onFailure = { error ->
                handleFlightSummaryError(
                    getUserIdFromContext(),
                    error,
                    "getAllFlightIds"
                )
                null
            }
        )
    }


    override suspend fun saveFlights(request: FlightRequest.Save): List<FlightSummaryModel>? {
        return runCatching {
            // Step 1: Save flight summaries
            val flightSummaryEntities = flightSummaryMapper.fromModelList(request.flights)
            val savedSummaries = repository.saveFlightSummaries(flightSummaryEntities).getOrThrow()

            val validFlightIds = savedSummaries.flights.mapNotNull { it.fr24_id?.takeIf(String::isNotEmpty) }
            if (validFlightIds.isNotEmpty()) {
                val tracks = validFlightIds.map { FlightRequest.Track(it) }.mapNotNull { getFlightTrack(it) }
                if (tracks.isNotEmpty()) {
                    repository.saveTracks(tracks).getOrThrow()
                }
                // Step 3: Link flights to user
                repository.saveUserFlights(validFlightIds).getOrThrow()
            }
            flightSummaryMapper.fromEntityList(savedSummaries.flights)
        }.onFailure { error ->
            handleFlightSummaryError(getUserIdFromContext(), error, "saveFlights")
        }.getOrNull()
    }

    private fun handleFlightSummaryError(
        user: String,
        error: Throwable,
        operation: String,
    ) {
        logger.error("❌ Failed to $operation' for user $user: $error")
    }

}
