package repository

import AuthClient
import entity.FlightIdEntity
import entity.FlightSummaryEntity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import model.*
import util.FlightRequest
import io.ktor.util.logging.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import plugin.getUserIdFromContext
import service.FR24API

interface FlightRepository {
    suspend fun getAirline(icao: FlightRequest.Airline): Result<AirlinesLightModel?>
    suspend fun saveAirline(airline: AirlinesLightModel): Result<Unit>
    suspend fun saveUserFlights(flightIds: List<String>): Result<FlightSaveResult.UserFlightsSaved>
    suspend fun getFlightSummary(flight: FlightRequest.Summary): Result<List<FlightSummaryEntity>?>
    suspend fun saveFlightSummaries(flights: List<FlightSummaryEntity>): Result<FlightSaveResult.ListSaved>
    suspend fun getAllFlights(): Result<List<FlightSummaryEntity>?>
    suspend fun getAllFlightIds(): List<FlightRequest.Track>
    suspend fun getTrack(id: FlightRequest.Track): Result<FlightTracksModel?>
    suspend fun getAllTracks(flightIds: List<FlightRequest.Track>): Result<List<FlightTracksModel?>>
    suspend fun saveTracks(tracks: List<FlightTracksModel>): Result<Unit>
    suspend fun fetchTracksFromApi(missingFlightIds: List<FlightRequest.Track>): Map<String, FlightTracksModel?>
}

sealed class FlightSaveResult {
    data class ListSaved(val flights: List<FlightSummaryEntity>, val isNew: Boolean) : FlightSaveResult()
    data class UserFlightsSaved(val flights: List<String>, val isNew: Boolean) : FlightSaveResult()
}

class FlightDataRepository(
    authClient: AuthClient,
    private val logger: Logger,
    private val api: FR24API
) : FlightRepository {
    private val client = authClient.createUnauthenticatedClient()

    companion object {
        private const val TABLE_USER_FLIGHTS = "UserFlights"
        private const val TABLE_FLIGHT_SUMMARY = "FlightSummary"
        private const val TABLE_FLIGHT_TRACKS = "FlightTracks"
        private const val EMPTY_RESPONSE = "[]"
    }

    override suspend fun getAirline(icao: FlightRequest.Airline): Result<AirlinesLightModel?> =
        Result.success(null)

    override suspend fun saveAirline(airline: AirlinesLightModel): Result<Unit> =
        Result.success(Unit)

    override suspend fun saveFlightSummaries(
        flights: List<FlightSummaryEntity>
    ): Result<FlightSaveResult.ListSaved> = runCatching {

        client.from(TABLE_FLIGHT_SUMMARY).upsert(flights)
        FlightSaveResult.ListSaved(flights, isNew = true)

    }.onFailure { e ->
        logger.error("Failed to save flight summaries", e)
    }

    override suspend fun saveUserFlights(
        flightIds: List<String>
    ): Result<FlightSaveResult.UserFlightsSaved> = runCatching {
        val userFlights = flightIds.map { entity ->
            mapOf(
                "user_id" to getUserIdFromContext(),
                "flight_id" to entity
            )
        }
        client.from(TABLE_USER_FLIGHTS).upsert(userFlights)
        FlightSaveResult.UserFlightsSaved(flightIds, isNew = true)
    }

    override suspend fun getFlightSummary(
        flight: FlightRequest.Summary
    ): Result<List<FlightSummaryEntity>?> = runCatching {
        val response = withContext(Dispatchers.IO) {
            client.from(TABLE_FLIGHT_SUMMARY).select {
                filter {
                    eq("callsign", flight.callsign)
                    gte("datetime_takeoff", "${flight.datetimeFrom}T00:00:00Z")
                    lte("datetime_takeoff", "${flight.datetimeFrom}T23:59:59.999Z")
                    gte("datetime_landed", "${flight.datetimeTo}T00:00:00Z")
                    lte("datetime_landed", "${flight.datetimeTo}T23:59:59.999Z")
                }
            }
        }

        val flightSummaryEntities: List<FlightSummaryEntity> = Json.decodeFromString(response.data)

        if (flightSummaryEntities.isNotEmpty()) {
            // Data found in DB
            logger.info("Flight Summary retrieved from DB")
            flightSummaryEntities
        } else {
            logger.info("No flight summary data found in DB, fetching from API")
            val flights = api.flightSummaryFull(flight)
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
        val response = withContext(Dispatchers.IO) {
            client.postgrest[TABLE_USER_FLIGHTS]
                .select(columns = Columns.raw("""$TABLE_FLIGHT_SUMMARY(*)""")) {
                    filter {
                        eq("user_id", getUserIdFromContext())
                    }
                }
        }

        val flightSummaryEntities: List<FlightSummaryEntity> = Json.decodeFromString<List<JsonElement>>(response.data)
            .mapNotNull { jsonElement ->
                jsonElement.jsonObject[TABLE_FLIGHT_SUMMARY]?.let { inner ->
                    Json.decodeFromJsonElement<FlightSummaryEntity>(inner)
                }
            }

        flightSummaryEntities
    }.onFailure { e ->
        logger.error("Error retrieving all flights", e)
    }

    override suspend fun getAllFlightIds(): List<FlightRequest.Track> {
        val response = withContext(Dispatchers.IO) {
            client.from(TABLE_USER_FLIGHTS).select(columns = Columns.raw("flight_id")) {
                filter {
                    eq("user_id", getUserIdFromContext())
                }
            }
        }

        val flightTrackEntities: List<FlightIdEntity> = Json.decodeFromString(response.data)
        return flightTrackEntities.map { FlightRequest.Track(it.flight_id) }
    }

    override suspend fun getTrack(id: FlightRequest.Track): Result<FlightTracksModel?> = runCatching {
        val response = withContext(Dispatchers.IO) {
            client.from(TABLE_FLIGHT_TRACKS).select {
                filter {
                    eq("fr24_id", id)
                }
            }
        }

        // Check if response data is empty
        if (response.data == EMPTY_RESPONSE) {
            logger.info("No flight tracks found in DB, fetching from API")
            val flights = api.flightTracks(id)
            if (!flights?.fr24_id.isNullOrEmpty() && flights.tracks.isNotEmpty()) {
                flights
            } else {
                null
            }
        } else {
            // Only try to decode if we have data
            val flightTrackEntities: FlightTracksModel = Json.decodeFromString(response.data)

            if (flightTrackEntities.tracks.isNotEmpty()) {
                logger.info("Flight Tracks retrieved from DB")
                flightTrackEntities
            } else {
                logger.info("No flight tracks found in DB, fetching from API")
                val flights = api.flightTracks(id)
                if (!flights?.fr24_id.isNullOrEmpty() && flights.tracks.isNotEmpty()) {
                    flights
                } else {
                    null
                }
            }
        }
    }.onFailure { e ->
        logger.error("Error Retrieving Tracks", e)
    }

    override suspend fun getAllTracks(flightIds: List<FlightRequest.Track>): Result<List<FlightTracksModel?>> {
        return try {
            val dbResponse = withContext(Dispatchers.IO) {
                client.from(TABLE_FLIGHT_TRACKS).select {
                    filter {
                        "fr24_id" in flightIds.map { it.flightId }
                    }
                }
            }

            val dbTracks: List<FlightTracksModel> = dbResponse.decodeList<FlightTracksModel>()
            val trackMap: Map<String, FlightTracksModel> = dbTracks.associateBy { it.fr24_id }
            val missingFlightIds = flightIds.filter { trackMap[it.flightId] == null }
            val apiTracks = fetchTracksFromApi(missingFlightIds)

            // Combine results: DB hits + API hits/failures
            val allTracks: List<FlightTracksModel?> = flightIds.map { trackRequest ->
                (trackMap[trackRequest.flightId] ?: apiTracks[trackRequest.flightId])
            }

            saveTracks(apiTracks.values.filterNotNull())
            Result.success(allTracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveTracks(tracks: List<FlightTracksModel>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            client.postgrest.from(TABLE_FLIGHT_TRACKS)
                .upsert(tracks)
            tracks.forEach {
                logger.debug("Flight track ${it.fr24_id} saved to database")
            }
        }
    }.onFailure { e ->
        logger.error("Failed to save flight track: ${e.message}", e)
    }

    /**
     *  Need a way to batch send requests to FR24 api so that we don't overload requests per min (30).
     */

    override suspend fun fetchTracksFromApi(missingFlightIds: List<FlightRequest.Track>): Map<String, FlightTracksModel?> {
        val semaphore = Semaphore(permits = 5)

        return coroutineScope {
            val deferredResults = missingFlightIds.map { trackRequest ->
                async {
                    semaphore.withPermit {
                        println("Fetching missing track: ${trackRequest.flightId}")
                        val apiResult = runCatching { api.flightTracks(trackRequest) }.getOrNull()
                        trackRequest.flightId to apiResult
                    }
                }
            }
            deferredResults.awaitAll().toMap()
        }
    }
}