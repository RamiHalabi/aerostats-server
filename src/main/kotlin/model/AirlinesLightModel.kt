package model

import kotlinx.serialization.Serializable


@Serializable
data class AirlinesLightModel(
    val icao: String?,
    val iata: String?,
    val name: String?,
) {
    override fun toString(): String {
        return """
        ${icao}
        ${iata}
        ${name}
    """.trimIndent()
    }
}