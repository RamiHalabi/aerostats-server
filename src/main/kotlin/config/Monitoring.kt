package config

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.*

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val authorization = call.request.headers["Authorization"]
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent, Auth: $authorization"
        }
    }
}
