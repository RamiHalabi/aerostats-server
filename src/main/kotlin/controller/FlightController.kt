package controller

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import service.FlightService
import io.ktor.util.logging.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

fun ApplicationCall.getUserIdFromTokenSimple(): String? {
    val authHeader = request.header("Authorization")
    val token = if (authHeader?.startsWith("Bearer ") == true) {
        authHeader.substring(7)
    } else {
        return null
    }

    return try {
        // JWT format: header.payload.signature
        val parts = token.split(".")
        if (parts.size != 3) return null

        // Decode the payload (second part)
        val payload = String(Base64.getUrlDecoder().decode(parts[1]))
        println("🔍 JWT Payload: $payload")

        // Parse JSON and extract 'sub' field
        val json = Json.parseToJsonElement(payload).jsonObject
        json["sub"]?.jsonPrimitive?.content
    } catch (e: Exception) {
        println(e)
        null
    }
}


@kotlinx.serialization.Serializable
data class FlightDataRequest(val callsign: String)

fun Route.flightRoutes(service: FlightService, log: Logger) {

    log.info("Setting up flight routes")

    route("/flights") {
        get("/airline/{icao}") {
            val icao = call.parameters["icao"] ?: return@get call.respondText(
                "Missing ICAO",
                status = HttpStatusCode.BadRequest
            )
            val result = service.getAirlineLight(icao)
            call.respond(result)
        }

        post("/data/icao") {
            val request = call
            val userId = call.getUserIdFromTokenSimple()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or missing token"))

            val body = call.receive<FlightDataRequest>()
            log.info(userId)

            if(!body.callsign.matches(Regex("^[A-Z0-9]+$"))) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid call sign"))
            }

            try {
                log.info("Received request for flight data: $request")
                val response = service.getFlightData(body.callsign, userId)
                log.info("Response for flight data: $response")
                call.respond(
                    mapOf(
                        "fr24_id" to response?.fr24_id,
                        "flight" to response?.flight,
                        "callsign" to response?.callsign,
                        "timestamp" to response?.timestamp,
                        "type" to response?.type,
                        "reg" to response?.reg,
                        "painted_as" to response?.painted_as,
                        "operating_as" to response?.operating_as,
                        "orig_icao" to response?.orig_icao,
                        "dest_icao" to response?.dest_icao,
                    )
                )
                log.info("Returned Flight data request successfully")
            } catch (e: Exception) {
                log.error("Error getting flight data", e)
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