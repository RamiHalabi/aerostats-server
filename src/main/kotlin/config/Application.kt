package com.ramihalabi.config

import AuthClient
import com.ramihalabi.event.Event
import com.ramihalabi.event.EventBus
import config.configureRouting
import io.ktor.server.application.*
import repository.FlightDataRepository
import service.FR24API
import service.FlightService


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    log.info("Application module starting...")

    EventBus.subscribe { event ->
        when (event) {
            is Event.AirlineSaved -> log.info("New airline saved: ${event.airline.name}")
            is Event.FlightSaved -> log.info("New flight saved: ${event.flight.flight}")
            is Event.TrackSaved -> log.info("New track saved: ${event.track.tracks}")
        }
    }
    val api = FR24API()
    val repository = FlightDataRepository()
    val service = FlightService(api, repository)
    val authClient = AuthClient()
    configureRouting(authClient, service, log)
    configureMonitoring()
    configureSerialization()
    configureSecurity()
}
