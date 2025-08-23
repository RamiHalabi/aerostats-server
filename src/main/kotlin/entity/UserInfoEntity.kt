package entity

import kotlinx.serialization.Serializable

@Serializable
data class UserInfoEntity(
    val user_id: String,
    val timezone: String,
    val home_base: String,
    val age: String,
    val airline: String,
    val username: String,
)
