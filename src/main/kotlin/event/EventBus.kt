package com.ramihalabi.event

import model.AirlinesLightModel
import model.FlightDataModel
import model.FlightTracksModel

object EventBus {
    private val listeners = mutableListOf<(Event) -> Unit>()

    fun post(event: Event) {
        listeners.forEach { it(event) }
    }

    fun subscribe(listener: (Event) -> Unit) {
        listeners.add(listener)
    }
}

sealed class Event {
    data class AirlineSaved(val airline: AirlinesLightModel) : Event()
    data class FlightSaved(val flight: FlightDataModel) : Event()
    data class TrackSaved(val track: FlightTracksModel) : Event()
}