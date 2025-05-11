package controller

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import service.FlightService
import io.ktor.util.logging.*


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

        get("/data/{flightNumber}") {
            val flightNumber = call.parameters["flightNumber"] ?: return@get call.respondText(
                "Missing Flight Number",
                status = HttpStatusCode.BadRequest
            )
            val result = service.getFlightData(flightNumber)
            call.respond(result ?: "No Flight found")
        }

        get("/track/{id}") {
            val id =
                call.parameters["id"] ?: return@get call.respondText("Missing ID", status = HttpStatusCode.BadRequest)
            val result = service.getFlightTrack(id)
            call.respond(result ?: "No track found")
        }
    }
}