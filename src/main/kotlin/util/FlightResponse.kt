package util

import Entity.FlightSummaryEntity
import kotlinx.serialization.Serializable


@Serializable
sealed class FlightResponse {
    @Serializable
    data class Summary(
        val data: List<FlightSummaryEntity>
    ) : FlightResponse()
}
