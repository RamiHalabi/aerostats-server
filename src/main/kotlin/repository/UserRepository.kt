package repository

import AuthClient
import Entity.UserInfoEntity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mapper.UserInfoMapper
import model.UserInfoModel

interface IUserRepository {
    suspend fun getUserInfo(token: String): UserInfoModel?
}

class UserRepository(
    authClient: AuthClient,
) : IUserRepository {
    private val client = authClient.createUnauthenticatedClient()
    private val mapper =  UserInfoMapper()

    /**
     * Fetches user information based on their UUID. The method retrieves data from
     * supabase, maps the retrieved UserInfoEntity to a UserInfoModel, and returns the result.
     *
     * @param token A string representing the user's UUID used to locate the user's information.
     * @return A [UserInfoModel] containing user details if found, or null if no user is found or in case of an error.
     */
    override suspend fun getUserInfo(token: String): UserInfoModel? {
        val response = withContext(Dispatchers.IO) {
            client.from("UserInfo").select {
                filter {
                    eq("user_id", token)
                }
            }
        }

        val userInfoEntity: UserInfoEntity = try {
            val userList = Json.decodeFromString<List<UserInfoEntity>>(response.data)
            if (userList.isEmpty()) {
                println("No user found with token: $token")
                return null
            }

            userList[0] // Get the first item in the list
        } catch (e: Exception) {
            println("Error parsing UserInfo: ${e.message}")
            return null
        }

        return mapper.fromEntity(userInfoEntity)
    }
}