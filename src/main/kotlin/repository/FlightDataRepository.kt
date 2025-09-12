package repository

import dao.FlightDao
import entity.FlightSummaryEntity
import io.ktor.util.logging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import model.AirlinesLightModel
import model.FlightTracksModel
import plugin.getUserIdFromContext
import service.FR24API
import util.FlightRequest

interface FlightRepository {
    suspend fun getAirline(icao: FlightRequest.Airline): Result<AirlinesLightModel?>
    suspend fun saveAirline(airline: AirlinesLightModel): Result<Unit>
    suspend fun saveUserFlights(flightIds: List<String>): Result<FlightSaveResult.UserFlightsSaved>
    suspend fun getFlightSummary(flight: FlightRequest.Summary): Result<List<FlightSummaryEntity>?>
    suspend fun saveFlightSummaries(flights: List<FlightSummaryEntity>): Result<FlightSaveResult.ListSaved>
    suspend fun getAllFlights(): Result<List<FlightSummaryEntity>?>
    suspend fun getAllFlightIds(): Result<List<FlightRequest.Track>?>
    suspend fun getTrack(id: FlightRequest.Track): Result<FlightTracksModel?>
    suspend fun getAllTracks(flightIds: List<FlightRequest.Track>): Result<List<FlightTracksModel?>>
    suspend fun saveTracks(tracks: List<FlightTracksModel>): Result<Unit>
}

sealed class FlightSaveResult {
    data class ListSaved(val flights: List<FlightSummaryEntity>, val isNew: Boolean) : FlightSaveResult()
    data class UserFlightsSaved(val flights: List<String>, val isNew: Boolean) : FlightSaveResult()
}

class FlightDataRepository(
    private val logger: Logger, private val flightDao: FlightDao, private val fr24api: FR24API
) : FlightRepository {

    override suspend fun getAirline(icao: FlightRequest.Airline): Result<AirlinesLightModel?> = Result.success(null)

    override suspend fun saveAirline(airline: AirlinesLightModel): Result<Unit> = Result.success(Unit)

    override suspend fun saveFlightSummaries(
        flights: List<FlightSummaryEntity>
    ): Result<FlightSaveResult.ListSaved> = runCatching {
        flightDao.saveFlights(flights)
        FlightSaveResult.ListSaved(flights, isNew = true)
    }.onFailure { e ->
        logger.error("Failed to save flight summaries", e)
    }

    override suspend fun saveUserFlights(
        flightIds: List<String>
    ): Result<FlightSaveResult.UserFlightsSaved> = runCatching {
        val userFlights = flightIds.map { entity ->
            mapOf(
                "user_id" to getUserIdFromContext(), "flight_id" to entity
            )
        }
        flightDao.saveUserFlights(userFlights)
        FlightSaveResult.UserFlightsSaved(flightIds, isNew = true)
    }.onFailure { e ->
        logger.error("Error saving user flight", e)
    }

    override suspend fun getFlightSummary(
        flight: FlightRequest.Summary
    ): Result<List<FlightSummaryEntity>?> = runCatching {
        val dbResponse = flightDao.getFlightSummariesByCallsign(flight)
        dbResponse ?: run {
            logger.info("No flight summary data found in DB, fetching from API")
            val flights = fr24api.flightSummaryFull(flight)
            if (!flights.isNullOrEmpty()) {
                flights
            } else {
                emptyList()
            }
        }
    }.onFailure { e ->
        logger.error("Error retrieving flight summary", e)
    }

    override suspend fun getAllFlights(): Result<List<FlightSummaryEntity>?> = runCatching {
        flightDao.getAllFlights()
    }.onFailure { e ->
        logger.error("Error retrieving all flights", e)
    }

    override suspend fun getAllFlightIds(): Result<List<FlightRequest.Track>?> = runCatching {
        flightDao.getAllFlightIds().map { FlightRequest.Track(it.flight_id) }
    }.onFailure { e ->
        logger.error("Error retrieving all flight ids", e)
    }

    override suspend fun getTrack(id: FlightRequest.Track): Result<FlightTracksModel?> = runCatching {
        val dbResponse = flightDao.getFlightTrack(id)

        dbResponse ?: run {
            logger.info("No flight tracks found in DB, fetching from API")
            val apiTrack = fr24api.flightTracks(id)
            if (apiTrack?.fr24_id?.isNotEmpty() == true && apiTrack.tracks.isNotEmpty()) {
                flightDao.saveFlightTracks(listOf(apiTrack))
                apiTrack
            } else {
                null
            }
        }
    }.onFailure { e ->
        logger.error("Error Retrieving Tracks for flight ${id.flightId}", e)
    }

    override suspend fun getAllTracks(flightIds: List<FlightRequest.Track>): Result<List<FlightTracksModel?>> {
        return try {
            val dbTracks = flightDao.getMultipleFlightTracks(flightIds)
            val trackMap: Map<String, FlightTracksModel>? = dbTracks?.associateBy { it.fr24_id }
            val missingFlightIds = flightIds.filter { trackMap?.get(it.flightId) == null }
            val apiTracks = fetchTracksFromApi(missingFlightIds)

            // Combine results: DB hits + API hits/failures
            val allTracks: List<FlightTracksModel?> = flightIds.map { trackRequest ->
                (trackMap?.get(trackRequest.flightId) ?: apiTracks[trackRequest.flightId])
            }

            saveTracks(apiTracks.values.filterNotNull())
            Result.success(allTracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveTracks(tracks: List<FlightTracksModel>): Result<Unit> = runCatching {
        flightDao.saveFlightTracks(tracks)
        tracks.forEach {
            logger.debug("Flight track ${it.fr24_id} saved to database")
        }
    }.onFailure { e ->
        logger.error("Failed to save flight track: ${e.message}", e)
    }

    /**
     *  Need a way to batch send requests to FR24 api so that we don't overload requests per min (30).
     *  Move api calls to a separate class and use coroutines to batch requests.
     */
    private suspend fun fetchTracksFromApi(missingFlightIds: List<FlightRequest.Track>): Map<String, FlightTracksModel?> =
        withContext(Dispatchers.IO) {
            val semaphore = Semaphore(10)
            missingFlightIds.map { id ->
                async {
                    semaphore.withPermit {
                        id.flightId to fr24api.flightTracks(id)
                    }
                }
            }.awaitAll().toMap()
        }
}