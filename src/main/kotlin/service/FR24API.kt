package service

import entity.FlightSummaryEntity
import config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import model.*
import util.DateUtil
import util.FlightRequest
import util.FlightResponse
import java.net.HttpURLConnection
import java.net.URL

class FR24API {
    /**
     * Companion object for `FR24API` class.
     * Contains constants and utility functions for making HTTP requests to the FR24 API.
     *
     * This object defines the necessary configuration for the API, including authorization headers,
     * base URLs, and endpoints required for various API calls. It also includes functionality
     * for managing HTTP GET requests.
     */
    companion object {
        private val API_TOKEN = Config.FR24_API_KEY
        private val BASE_URL = Config.FR24_URL;
        private val GET_HEADERS = mapOf(
            "Accept" to "application/json",
            "Authorization" to "Bearer $API_TOKEN",
            "Accept-Version" to "v1"
        )
        private val AIRLINES_LIGHT_URL = "/static/airlines/{icao}/light"
        private val FLIGHT_SUMMARY_FULL_URL =
            "/flight-summary/full?flight_datetime_from={dateTimeFrom}&flight_datetime_to={dateTimeTo}&callsigns={callsigns}&limit=100"
        private val FLIGHT_TRACKS_URL = "/flight-tracks?flight_id={flightID}&limit=100"
        private val json = Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }
        private suspend fun GET(url: String): String? {
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
    }

    /**
     * Fetches airline information based on the provided ICAO code.
     *
     * @param icao The ICAO code of the airline for which information is to be fetched.
     * @return An instance of AirlinesLightModel containing the airline's details,
     *         or null if the data is unavailable or an error occurs.
     */
    suspend fun airlinesLight(icao: String): AirlinesLightModel? {
        val url = "$BASE_URL${AIRLINES_LIGHT_URL.replace("{icao}", icao)}"
        val response: AirlinesLightModel? = GET(url)?.let { json.decodeFromString<AirlinesLightModel>(it) }
        return response
    }

    /**
     * Fetches a full flight summary for a given flight based on its callsign and date range.
     *
     * @param flight The request object containing the flight's callsign, start datetime (datetimeFrom), and end datetime (datetimeTo).
     *               These parameters are required to build the query for retrieving flight summaries.
     * @return A list of flight summary entities representing the relevant flight details, or null if no data is available or an error occurs.
     */
    suspend fun flightSummaryFull(flight: FlightRequest.Summary): List<FlightSummaryEntity>? {
        val dateUtil = DateUtil()
        val url = "$BASE_URL${FLIGHT_SUMMARY_FULL_URL
                .replace("{callsigns}", flight.callsign)
                .replace("{dateTimeFrom}", dateUtil.formatToDateTimeStamp(flight.datetimeFrom, false))
                .replace("{dateTimeTo}", dateUtil.formatToDateTimeStamp(flight.datetimeTo, true))
        }"
        /**
          TODO: MULTIPLE FLIGHTS RETURNS FINE W NULL BUT IF ONLY 1 FLIGHT, ERRORS OUT AND RETURNS NOTHING.
         */
        try{
        val response = GET(url)?.let { json.decodeFromString<FlightResponse.Summary>(it) }
        println("Flight Summary retrieved from API")
        return response?.data
        } catch (e: Exception) {
            println(e)
        }
        return null
    }

    /**
     * Fetches flight tracking details for a specific flight using its flight ID.
     *
     * @param flight The request containing the flight ID for which the tracking details are to be fetched.
     * @return The flight tracking details as a FlightTracksModel, or null if no tracking data is found.
     */
    suspend fun flightTracks(flight: FlightRequest.Track): FlightTracksModel? {
        val url = "$BASE_URL${FLIGHT_TRACKS_URL.replace("{flightID}", flight.flightId)}"
        val response: List<FlightTracksModel>? = GET(url)?.let { json.decodeFromString(it) }
        return response?.firstOrNull()
    }
}
