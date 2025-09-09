package dao

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
import model.FlightTracksModel
import plugin.getUserIdFromContext
import util.FlightRequest
import kotlin.collections.ifEmpty

interface FlightDao {
    suspend fun saveFlights(flights: List<FlightSummaryEntity>)
    suspend fun saveUserFlights(userFlights: List<Map<String, String>>)
    suspend fun getFlightSummariesByCallsign(flight: FlightRequest.Summary): List<FlightSummaryEntity>?
    suspend fun getAllFlights(): List<FlightSummaryEntity>?
    suspend fun getAllFlightIds(): List<FlightIdEntity>
    suspend fun getFlightTrack(id: FlightRequest.Track): FlightTracksModel?
    suspend fun getMultipleFlightTracks(flightIds: List<FlightRequest.Track>): List<FlightTracksModel>?
    suspend fun saveFlightTracks(tracks: List<FlightTracksModel>)
}

class SupabaseFlightDao(
    authClient: AuthClient,
) : FlightDao {
    private val client = authClient.createUnauthenticatedClient()

    companion object {
        private const val TABLE_USER_FLIGHTS = "UserFlights"
        private const val TABLE_FLIGHT_SUMMARY = "FlightSummary"
        private const val TABLE_FLIGHT_TRACKS = "FlightTracks"
        private const val EMPTY_RESPONSE = "[]"
    }

    override suspend fun saveFlights(
        flights: List<FlightSummaryEntity>
    ) {
        withContext(Dispatchers.IO) { client.from(TABLE_FLIGHT_SUMMARY).upsert(flights) }
    }

    override suspend fun saveUserFlights(userFlights: List<Map<String, String>>) {
        withContext(Dispatchers.IO) { client.from(TABLE_USER_FLIGHTS).upsert(userFlights) }
    }

    override suspend fun getFlightSummariesByCallsign(
        flight: FlightRequest.Summary
    ): List<FlightSummaryEntity>? {
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

        if (response.data == EMPTY_RESPONSE) {
            return null
        }

        val flightSummaryEntities: List<FlightSummaryEntity> = Json.decodeFromString(response.data)
        return flightSummaryEntities.ifEmpty {
            null
        }
    }

    override suspend fun getAllFlights(): List<FlightSummaryEntity>? {
        val response = withContext(Dispatchers.IO) {
            client.postgrest[TABLE_USER_FLIGHTS]
                .select(columns = Columns.raw("""$TABLE_FLIGHT_SUMMARY(*)""")) {
                    filter {
                        eq("user_id", getUserIdFromContext())
                    }
                }
        }

        if (response.data == EMPTY_RESPONSE) {
            return null
        }

        val flightSummaryEntities: List<FlightSummaryEntity> = Json.decodeFromString<List<JsonElement>>(response.data)
            .mapNotNull { jsonElement ->
                jsonElement.jsonObject[TABLE_FLIGHT_SUMMARY]?.let { inner ->
                    Json.decodeFromJsonElement<FlightSummaryEntity>(inner)
                }
            }

        return flightSummaryEntities.ifEmpty {
            null
        }
    }

    override suspend fun getAllFlightIds(): List<FlightIdEntity> {
        val response = withContext(Dispatchers.IO) {
            client.from(TABLE_USER_FLIGHTS).select(columns = Columns.raw("flight_id")) {
                filter {
                    eq("user_id", getUserIdFromContext())
                }
            }
        }

        val flightTrackEntities: List<FlightIdEntity> = Json.decodeFromString(response.data)
        return flightTrackEntities
    }

    override suspend fun getFlightTrack(id: FlightRequest.Track): FlightTracksModel? {
        val response = withContext(Dispatchers.IO) {
            client.from(TABLE_FLIGHT_TRACKS).select {
                filter {
                    eq("fr24_id", id.flightId)
                }
            }
        }

        if (response.data == EMPTY_RESPONSE) {
            return null
        }

        val trackModel: FlightTracksModel = Json.decodeFromString(response.data)
        return if (trackModel.tracks.isNotEmpty()) {
            trackModel
        } else {
            null
        }
    }

    override suspend fun getMultipleFlightTracks(flightIds: List<FlightRequest.Track>): List<FlightTracksModel>? {
        val response = withContext(Dispatchers.IO) {
            client.from(TABLE_FLIGHT_TRACKS).select {
                filter {
                    flightIds.map { it.flightId }.contains("fr24_id")
                }
            }
        }

        if (response.data == EMPTY_RESPONSE) {
            return null
        }

        val flightTracks: List<FlightTracksModel> = response.decodeList<FlightTracksModel>()
        return flightTracks.ifEmpty {
            null
        }
    }

    override suspend fun saveFlightTracks(tracks: List<FlightTracksModel>) {
        withContext(Dispatchers.IO) {
            client.postgrest.from(TABLE_FLIGHT_TRACKS)
                .upsert(tracks)
        }
    }
}