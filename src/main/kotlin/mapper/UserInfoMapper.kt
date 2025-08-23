package mapper

import entity.UserInfoEntity
import model.UserInfoModel

class UserInfoMapper {
    fun fromModel(model: UserInfoModel): UserInfoEntity {
        return UserInfoEntity(
            user_id = model.userId,
            timezone = model.timezone,
            home_base = model.homeBase,
            age = model.age,
            airline = model.airline,
            username = model.username
        )
    }

    fun fromEntity(entity: UserInfoEntity): UserInfoModel {
        return UserInfoModel(
            userId = entity.user_id,
            timezone = entity.timezone,
            homeBase = entity.home_base,
            age = entity.age,
            airline = entity.airline,
            username = entity.username
        )
    }

    fun fromModelList(models: List<UserInfoModel>): List<UserInfoEntity> {
        return models.map { fromModel(it) }
    }

    fun fromEntityList(entities: List<UserInfoEntity>): List<UserInfoModel> {
        return entities.map { fromEntity(it) }
    }

}
