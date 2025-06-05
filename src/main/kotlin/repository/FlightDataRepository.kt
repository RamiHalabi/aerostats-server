package repository

import AuthClient
import controller.FlightDataRequest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mapper.FlightDataEntity
import mapper.FlightDataMapper
import mapper.FlightSummaryMapper
import model.*
import util.DateUtil

sealed class FlightSaveResult {
    data class Saved(val flight: FlightData, val isNew: Boolean) : FlightSaveResult()
    data class Failed(val exception: Throwable) : FlightSaveResult()
}

class FlightDataRepository(authClient: AuthClient, private val dateUtil: DateUtil) {

    private val client = authClient.createUnauthenticatedClient()


    suspend fun getAirlineByIcao(icao: String): AirlinesLightModel? {
        // Query Supabase / Database
        return null
    }


    suspend fun getFlight(
        flight: FlightDataRequest
    ): FlightDataModel? {
        val response = withContext(Dispatchers.IO) {
            client.from("FlightData").select {
                filter {
                    eq("callsign", flight.callsign)
                    eq("orig_icao", flight.origIcao)
                    eq("dest_icao", flight.destIcao)
                    eq("date", flight.date)
                }
            }
        }

        val flightDataEntities: List<FlightDataEntity> = try {
            Json.decodeFromString(response.data)
        } catch (e: Exception) {
            println("Error parsing flight data: ${e.message}")
            return null
        }

        if (flightDataEntities.isEmpty()) {
            println("No flight data found")
            return null
        }

        return flightDataEntities.firstOrNull()?.let { entity ->
            FlightDataMapper.fromEntity(entity)
        }
    }

    suspend fun getUserFlight(
        flight: FlightDataRequest,
        userId: String,
    ): UserFlightsModel? {
        val response = withContext(Dispatchers.IO) {
              client.from("UserFlights").select {
                filter {
                    eq("callsign", flight.callsign)
                    eq("user_id", userId)
                    eq("orig_icao", flight.origIcao)
                    eq("dest_icao", flight.destIcao)
                    eq("date", dateUtil.getLocalDate(flight.date))
                }
            }
        }

        if (response.data == "[]") {
            println("No user flight data found")
            return null
        }

        return null
    }

    suspend fun saveAirline(airline: AirlinesLightModel) {
        // Insert into Supabase / Database
    }

    suspend fun saveFlight(flight: FlightDataModel, authToken: String): Result<FlightSaveResult.Saved> {
        val flightDataEntity = FlightDataMapper.fromModel(flight)
        flightDataEntity.apply {
            date = dateUtil.getLocalDate(date_added)
        }

        return runCatching {
            client.from("FlightData").upsert(listOf(flightDataEntity))

            val userFlight = mapOf(
                "user_id" to authToken,
                "flight_id" to flightDataEntity.flight_id,
                "callsign" to flight.callsign,
                "orig_icao" to flight.orig_icao,
                "dest_icao" to flight.dest_icao,
                "date" to flightDataEntity.date
            )
            client.from("UserFlights").upsert(listOf(userFlight))

            FlightSaveResult.Saved(flight, isNew = true)
        }
    }

    suspend fun saveFlightSummary(flight: FlightSummaryModel, authToken: String): Result<FlightSaveResult.Saved> {
        val flightSummary = FlightSummaryMapper.fromModel(flight)

        return runCatching {
            client.from("FlightSummary").upsert(flightSummary)

            FlightSaveResult.Saved(flight, isNew = true)
        }
    }

    suspend fun getFlightSummary(flightId: String): FlightSummaryModel? {
        val response = withContext(Dispatchers.IO) {
            client.from("FlightSummary").select {
                filter {
                    eq("flight_id", flightId)
                }
            }
        }

        if (response.data == "[]") {
            println("No flight summary found")
            return null
        }

        return null
    }


    fun getTrack(id: String): FlightTracksModel? {
        // Query Supabase / Database
        return null
    }

    fun saveTrack(it: FlightTracksModel) {

    }
}