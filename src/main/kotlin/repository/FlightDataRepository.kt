package repository

import AuthClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mapper.FlightDataMapper
import model.AirlinesLightModel
import model.FlightDataModel
import model.FlightTracksModel

sealed class FlightSaveResult {
    data class Saved(val flight: FlightDataModel, val isNew: Boolean) : FlightSaveResult()
    data class Failed(val exception: Throwable) : FlightSaveResult()
}

class FlightDataRepository(authClient: AuthClient) {

    val client = authClient.createUnauthenticatedClient()


    suspend fun getAirlineByIcao(icao: String): AirlinesLightModel? {
        // Query Supabase / Database
        return null
    }

    suspend fun getFlightByNumber(flight: String, userId: String): FlightDataModel? {
        val response = withContext(Dispatchers.IO) {
            client.from("UserFlights").select {
                filter {
                    eq("flight_id", flight)
                    eq("user_id", userId)
                }
            }
        }

        // Check if data is empty before trying to decode
        if (response.data == "[]") {
            println("No flight data found")
            return null
        }

        // Since you're filtering by specific IDs, you should get at most one result
        // Decode the first (and likely only) item from the array
        return null
    }

    suspend fun saveAirline(airline: AirlinesLightModel) {
        // Insert into Supabase / Database
    }

    suspend fun saveFlight(flight: FlightDataModel, authToken: String): Result<FlightSaveResult.Saved> {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Not logged in")
        val flightData = FlightDataMapper.fromModel(flight)

        return runCatching {
            // Insert or update flight
            client.from("FlightData").upsert(listOf(flightData))

            // Associate flight with user
            val userFlight = mapOf(
                "user_id" to authToken,
                "flight_id" to flightData.flight_id
            )
            client.from("UserFlights").upsert(listOf(userFlight))

            FlightSaveResult.Saved(flight, isNew = true)
        }
    }


    fun getTrack(id: String): FlightTracksModel? {
        // Query Supabase / Database
        return null
    }

    fun saveTrack(it: FlightTracksModel) {

    }
}