package config

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.*

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }

        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val path = call.request.path()
            val duration = call.processingTimeMillis()
            val userAgent = call.request.headers["User-Agent"] ?: "unknown"
            
            "Request: $httpMethod $path | Status: $status | Duration: ${duration}ms | User-Agent: $userAgent"
        }

        mdc("method") { call -> call.request.httpMethod.value }
        mdc("path") { call -> call.request.path() }
        mdc("status") { call -> call.response.status()?.value.toString() }
        mdc("duration") { call -> call.processingTimeMillis().toString() }
    }
}