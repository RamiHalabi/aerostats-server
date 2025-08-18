package config

import AuthClient
import auth.authRoutes
import controller.flightRoutes
import service.FlightService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import service.UserService

fun Application.configureRouting(
    authClient: AuthClient,
    userService: UserService,
    flightService: FlightService, log: Logger
) {
    try {
        routing {
            flightRoutes(flightService, log)
            authRoutes(
                authClient,
                userService,
                log
            )
        }
        log.info("Routing configured successfully")
    } catch (e: Throwable) {
        log.error("Error configuring routing", e)
    }
}