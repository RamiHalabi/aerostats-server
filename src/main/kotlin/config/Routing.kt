package com.ramihalabi.config

import controller.flightRoutes
import service.FlightService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(service: FlightService) {
    routing {
        flightRoutes(service)
    }
}