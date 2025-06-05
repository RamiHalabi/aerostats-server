package com.ramihalabi.config

import AuthClient
import event.Event
import event.EventBus
import config.configureMonitoring
import config.configureRouting
import io.ktor.server.application.*
import repository.FlightDataRepository
import service.FR24API
import service.FlightService
import util.DateUtil


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    log.info("Application module starting...")
    EventBus.subscribe { event ->
        when (event) {
            is Event.FlightSaved -> log.info(this.javaClass.name, "Flight saved: ${event.fr24_id}")
            is Event.FlightAlreadyKnown -> TODO()
            is Event.FlightSaveFailed -> TODO()
        }
    }
    val api = FR24API()
    val authClient = AuthClient()
    val dateUtil = DateUtil()
    val repository = FlightDataRepository(authClient, dateUtil)
    val service = FlightService(api, repository)
    configureRouting(authClient, service, log)
    configureMonitoring()
    configureSerialization()
    configureSecurity()
}
