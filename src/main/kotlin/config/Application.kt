package com.ramihalabi.config

import AuthClient
import config.configureMonitoring
import config.configureRouting
import config.configureSecurity
import io.ktor.server.application.*
import mapper.FlightSummaryMapper
import org.slf4j.Logger
import repository.FlightDataRepository
import repository.UserRepository
import service.FR24API
import service.FlightService
import service.UserService

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    log.info("Application module starting...")
    val appDependencies = AppDependencies(log)

    configureRouting(
        appDependencies.authClient,
        appDependencies.userService,
        appDependencies.flightService,
        log
    )
    configureMonitoring()
    configureSerialization()
    configureSecurity()

    log.info("Application module initialized successfully")
}

private class AppDependencies(log: Logger) {
    val api = FR24API()
    val authClient = AuthClient()
    val flightSummaryMapper = FlightSummaryMapper()
    val userRepository = UserRepository(authClient)
    val flightRepository = FlightDataRepository(authClient, log, api)
    val userService = UserService(userRepository)
    val flightService = FlightService(flightRepository, flightSummaryMapper, log)
}