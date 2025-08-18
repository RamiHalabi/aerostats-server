package repository

import AuthClient
import Entity.FlightSummaryEntity
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
import service.FR24API

interface FlightRepository {
    suspend fun getAirline(icao: FlightRequest.Airline): Result<AirlinesLightModel?>
    suspend fun saveAirline(airline: AirlinesLightModel): Result<Unit>
    suspend fun saveUserFlights(flightIds: List<String>, userId: String): Result<FlightSaveResult.UserFlightsSaved>
    suspend fun getFlightSummary(flight: FlightRequest.Summary, userId: String): Result<List<FlightSummaryEntity>?>
    suspend fun saveFlightSummaries(flights: List<FlightSummaryEntity>, userId: String): Result<FlightSaveResult.ListSaved>
    suspend fun getAllFlights(userId: String): Result<List<FlightSummaryEntity>?>
    suspend fun getTrack(id: FlightRequest.Track): Result<FlightTracksModel?>
    suspend fun getAllTracks(flightIds: List<FlightRequest.Track>): Result<List<FlightTracksModel?>>
    suspend fun saveTracks(tracks: List<FlightTracksModel>): Result<Unit>
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
        flights: List<FlightSummaryEntity>,
        userId: String
    ): Result<FlightSaveResult.ListSaved> = runCatching {

        client.from(TABLE_FLIGHT_SUMMARY).upsert(flights)
        FlightSaveResult.ListSaved(flights, isNew = true)

    }.onFailure { e ->
        logger.error("Failed to save flight summaries", e)
    }

    override suspend fun saveUserFlights(
        flightIds: List<String>,
        userId: String
    ): Result<FlightSaveResult.UserFlightsSaved> = runCatching {
        val userFlights = flightIds.map { entity ->
            mapOf(
                "user_id" to userId,
                "flight_id" to entity
            )
        }
        client.from(TABLE_USER_FLIGHTS).upsert(userFlights)
        FlightSaveResult.UserFlightsSaved(flightIds, isNew = true)
    }

    override suspend fun getFlightSummary(
        flight: FlightRequest.Summary,
        userId: String
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

    override suspend fun getAllFlights(userId: String): Result<List<FlightSummaryEntity>?> = runCatching {
        val response = withContext(Dispatchers.IO) {
            client.postgrest[TABLE_USER_FLIGHTS]
                .select(columns = Columns.raw("""$TABLE_FLIGHT_SUMMARY(*)""")) {
                    filter {
                        eq("user_id", userId)
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
            if (!flights?.fr24_id.isNullOrEmpty() && flights?.tracks?.isNotEmpty() == true) {
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
                if (!flights?.fr24_id.isNullOrEmpty() && flights?.tracks?.isNotEmpty() == true) {
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
            val result = mutableListOf<FlightTracksModel?>()

            // Process each flight ID
            for (trackRequest in flightIds) {
                val id = trackRequest.flightId

                // Try to get track from database first
                val dbResponse = withContext(Dispatchers.IO) {
                    client.from(TABLE_FLIGHT_TRACKS).select {
                        filter {
                            eq("fr24_id", id)
                        }
                    }
                }

                val track = dbResponse.decodeList<FlightTracksModel>().firstOrNull()

                if (track != null) {
                    result.add(track)
                } else {
                    val apiResult = runCatching { api.flightTracks(trackRequest) }
                    apiResult.onSuccess { result.add(it) }
                    apiResult.onFailure { result.add(null) }
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveTracks(tracks: List<FlightTracksModel>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            client.postgrest.from(TABLE_FLIGHT_TRACKS)
                .insert(tracks)
            tracks.forEach {
                logger.debug("Flight track ${it.fr24_id} saved to database")
            }
        }
    }.onFailure { e ->
        logger.error("Failed to save flight track: ${e.message}", e)
    }

}