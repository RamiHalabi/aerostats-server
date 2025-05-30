package config

import AuthClient
import auth.authRoutes
import controller.flightRoutes
import service.FlightService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*

fun Application.configureRouting(
    authClient: AuthClient,
    service: FlightService, log: Logger) {


    try {

    routing {
        flightRoutes(service, log)
        authRoutes(
            authClient,
            log
        )
    }
        log.info("Routing configured successfully")
    } catch (e: Throwable) {
        log.error("Error configuring routing", e)
    }
}