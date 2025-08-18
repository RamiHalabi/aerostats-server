package service

import model.UserInfoModel
import repository.UserRepository

class UserService(
    private val repository: UserRepository
) {
    suspend fun getUserInfo(uuid: String): UserInfoModel? {
        val result = repository.getUserInfo(uuid)
        return result
    }
}