package event

import model.*

object EventBus {
    private val listeners = mutableListOf<(Event) -> Unit>()

    fun post(event: Event) {
        listeners.forEach { it(event)
        print(it)
        }
    }

    fun subscribe(listener: (Event) -> Unit) {
        listeners.add(listener)
    }
}

sealed class Event {
    data class FlightSaved(val fr24_id: String) : Event()
    data class FlightAlreadyKnown(val flight: FlightDataModel) : Event()
    data class FlightSaveFailed(val flight: FlightData, val error: Throwable) : Event()
}