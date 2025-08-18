package model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class UserInfoModel(
    val userId: String,
    val timezone: String,
    val homeBase: String,
    val age: String,
    val airline: String,
    val username: String,
) {

    override fun toString(): String {
        return "UserInfoModel(id=${userId}, timezone='$timezone', homeBase='$homeBase', age='$age', airline='$airline', username='$username')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as UserInfoModel
        return userId == other.userId &&
                timezone == other.timezone &&
                homeBase == other.homeBase &&
                age == other.age &&
                airline == other.airline &&
                username == other.username
    }

    override fun hashCode(): Int {
        return Objects.hash(userId, timezone, homeBase, age, airline, username)
    }

    fun isValid(): Boolean {
        return userId.isNotBlank() &&
                timezone.isNotBlank() &&
                homeBase.isNotBlank() &&
                age.toIntOrNull() != null &&
                airline.isNotBlank() &&
                username.isNotBlank()
    }

    fun getDisplayInfo(): String {
        return "User Info: $username, Age: $age, Home Base: $homeBase, Airline: $airline"
    }
}