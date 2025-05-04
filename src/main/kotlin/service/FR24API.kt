package service

import config.Config
import model.AirlinesLightModel
import model.FlightDataModel
import model.FlightDataResponse
import model.FlightTracksModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class FR24API {
    companion object {
        private val API_TOKEN = Config.FR24_API_KEY
        private val BASE_URL = Config.FR24_URL;
        private val GET_HEADERS = mapOf(
            "Accept" to "application/json",
            "Authorization" to "Bearer $API_TOKEN",
            "Accept-Version" to "v1"
        )
        private val AIRLINES_LIGHT_URL = "/static/airlines/{icao}/light"
        private val FLIGHT_POSITIONS_FULL_URL = "/live/flight-positions/full?callsigns={flight}&limit=100"
        private val FLIGHT_TRACKS_URL = "/flight-tracks?flight_id={flightID}&limit=100"
        private val json = Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }
    }

    private suspend fun GET(url: String) : String? {
            return withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    for ((key, value) in GET_HEADERS) {
                        connection.setRequestProperty(key, value)
                    }
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw Exception("Error: ${connection.responseCode} - ${connection.responseMessage}")
                    }
                    connection.inputStream.bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    println(e)
                    null
                }
            }
        }

    suspend fun airlinesLight(icao: String): AirlinesLightModel? {
        val url = "$BASE_URL${AIRLINES_LIGHT_URL.replace("{icao}", icao)}"
        val response: AirlinesLightModel? = GET(url)?.let { json.decodeFromString<AirlinesLightModel>(it) }
        return response
    }

    suspend fun flightPositionsFull(flight: String): FlightDataModel? {
        val url = "$BASE_URL${FLIGHT_POSITIONS_FULL_URL.replace("{flight}", flight)}"
        val response = GET(url)?.let { json.decodeFromString<FlightDataResponse>(it) }
        return response?.data?.firstOrNull()
    }

    suspend fun flightTracks(flightID: String): FlightTracksModel? {
        val url = "$BASE_URL${FLIGHT_TRACKS_URL.replace("{flightID}", flightID)}"
        val response: List<FlightTracksModel>? = GET(url)?.let { json.decodeFromString(it) }
        return response?.firstOrNull()
    }
}
