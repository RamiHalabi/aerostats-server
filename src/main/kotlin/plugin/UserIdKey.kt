package plugin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

val UserIdKey = AttributeKey<String>("UserId")

fun ApplicationCall.getUserId(): String {
    return attributes[UserIdKey]
}

/**
 * A simple auth plugin that extracts the Bearer token, decodes it,
 * and injects the userId into the call context.
 */
fun authenticationPlugin() = createRouteScopedPlugin("AuthenticationPlugin") {
    onCall { call ->
        val userId = call.getUserIdFromTokenSimple()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or missing token"))
            return@onCall
        }
        call.attributes.put(UserIdKey, userId)
    }
}

fun ApplicationCall.getUserIdFromTokenSimple(): String? {
    val authHeader = request.header("Authorization")
    val token = if (authHeader?.startsWith("Bearer ") == true) {
        authHeader.substring(7)
    } else {
        return null
    }

    return try {
        val parts = token.split(".")
        if (parts.size != 3) return null
        val payload = String(Base64.getUrlDecoder().decode(parts[1]))
        val json = Json.parseToJsonElement(payload).jsonObject
        json["sub"]?.jsonPrimitive?.content
    } catch (e: Exception) {
        println(e)
        null
    }
}