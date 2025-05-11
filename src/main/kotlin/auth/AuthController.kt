package auth

import AuthClient
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*

@kotlinx.serialization.Serializable
data class SignInRequest(val email: String, val password: String)

@kotlinx.serialization.Serializable
data class NewAccessToken(val refreshToken: String)


fun Route.authRoutes(authClient: AuthClient, log: Logger) {

    log.info("Setting up auth routes")

    route("/auth") {
        post("/login") {
            val request = call.receive<SignInRequest>()
            try {
                val session = authClient.signIn(request.email, request.password)
                call.respond(
                    mapOf(
                        "access_token" to session["access_token"],
                        "refresh_token" to session["refresh_token"],
                        "user" to session["uuid"]
                    )
                )
                log.info("User logged in: ${session["uuid"]}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.localizedMessage))
            }
        }

        post("/refresh") {
            val request = call.receive<NewAccessToken>()
            try {
                val session = authClient.newAccessToken(request.refreshToken)
                call.respond(
                    mapOf(
                        "access_token" to session["access_token"],
                        "refresh_token" to session["refresh_token"]
                    )
                )
                log.info("Access token refreshed for user: ${session["uuid"]}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.localizedMessage))
            }
        }


        get("/logout") {

        }
    }
}