import config.Config
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

class AuthClient {

    private val logger = LoggerFactory.getLogger(AuthClient::class.java)

    private fun createUnauthenticatedClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = Config.SUPABASE_URL,
            supabaseKey = Config.SUPABASE_KEY
        ) {
            install(Auth) {
                // no session here
            }
        }
    }

    suspend fun signIn(email: String, password: String): JsonObject {
        val client = createUnauthenticatedClient()

        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }

        val uuid = client.auth.currentUserOrNull()?.id ?: "null"
        val accessToken = client.auth.currentAccessTokenOrNull() ?: "null"
        val refreshToken = client.auth.currentSessionOrNull()?.refreshToken ?: "null"

        return buildJsonObject {
            put("access_token", accessToken)
            put("refresh_token", refreshToken)
            put("uuid", uuid)
        }
    }

    suspend fun newAccessToken(refreshToken: String): JsonObject {
        val client = createUnauthenticatedClient()
        client.auth.refreshSession(refreshToken)
        val accessToken = client.auth.currentAccessTokenOrNull() ?: "null"
        val newRefreshToken = client.auth.currentSessionOrNull()?.refreshToken ?: "null"
        val uuid = client.auth.currentUserOrNull()?.id ?: "null"

        return buildJsonObject {
            put("access_token", accessToken)
            put("refresh_token", newRefreshToken)
            put("uuid", uuid)
        }
    }
}
