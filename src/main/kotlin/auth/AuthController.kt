package auth

import AuthClient
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import model.UserInfoModel
import service.UserService

@Serializable
data class SignInRequest(val email: String, val password: String)

@Serializable
data class NewAccessToken(val refreshToken: String)

@Serializable
data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val user: UserInfoModel?
)



fun Route.authRoutes(authClient: AuthClient, userService: UserService, log: Logger) {

    log.info("Setting up auth routes")

    route("/auth") {

        post("/apple") {
            val body = call.receive<Map<String, String>>()
            val idToken = body["id_token"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            try {
                val session = authClient.signInWithApple(idToken)
//                call.respond(
//
//                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, "Apple token invalid or expired")
            }
        }

        post("/login") {
            val request = call.receive<SignInRequest>()
            try {
                val session = authClient.signIn(request.email, request.password)

                val uuid = session["uuid"] ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val sanitizedUUID = uuid.toString().trim('"')

                val userInfo = userService.getUserInfo(sanitizedUUID)

                val accessToken = session["access_token"]?.toString()?.trim('"') ?: ""
                val refreshToken = session["refresh_token"]?.toString()?.trim('"') ?: ""

                val response = LoginResponse(
                    access_token = accessToken,
                    refresh_token = refreshToken,
                    user = userInfo
                )

                call.respond(response)
                log.info("User logged in: $sanitizedUUID")
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