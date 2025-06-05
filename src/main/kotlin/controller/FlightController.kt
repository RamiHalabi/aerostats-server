package controller

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import service.FlightService
import io.ktor.util.logging.*
import plugin.authenticationPlugin
import plugin.getUserId


@kotlinx.serialization.Serializable
data class FlightDataRequest(
    val callsign: String,
    val origIcao: String,
    val destIcao: String,
    val date: String,
)

data class FlightSummaryRequest(val flightId: String)

fun Route.flightRoutes(service: FlightService, log: Logger) {

    log.info("Setting up flight routes")

    route("/flights") {
        install(authenticationPlugin())

        get("/airline/{icao}") {
            val icao = call.parameters["icao"] ?: return@get call.respondText(
                "Missing ICAO",
                status = HttpStatusCode.BadRequest
            )
            val result = service.getAirlineLight(icao)
            call.respond(result)
        }

        post("/data") {
            val userId = call.getUserId()
            val body = call.receive<FlightDataRequest>()
            if (!body.callsign.matches(Regex("^[A-Z0-9]+$"))) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid call sign"))
            }

            try {
                log.info("Received request for flight data: $call")
                val response = service.getFlightData(body, userId)
                log.info("Response for flight data: $response")
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flight data not found"))
                }
                log.info("Returned Flight data request successfully")
            } catch (e: Exception) {
                log.error("Error getting flight data", e)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.localizedMessage))
            }
        }

        post("/summary") {
            val userId = call.getUserId()
            val body = call.receive<FlightSummaryRequest>()
            if (!body.flightId.matches(Regex("^[A-Za-z0-9]+$"))) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid flight ID"))
            }

            try {
                log.info("Received request for flight summary: $call")
                val response = service.getFlightSummary(body.flightId, userId)
                log.info("Response for flight summary: $response")
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flight summary not found"))
                }
                log.info("Returned Flight summary request successfully")
            } catch (e: Exception) {
                log.error("Error getting flight summary", e)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.localizedMessage))
            }
        }


        get("/track/{id}") {
            val id =
                call.parameters["id"] ?: return@get call.respondText("Missing ID", status = HttpStatusCode.BadRequest)
            val result = service.getFlightTrack(id)
            call.respond(result ?: "No track found")
        }
    }
}