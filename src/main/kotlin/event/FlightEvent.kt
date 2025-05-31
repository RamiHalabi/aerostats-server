package com.ramihalabi.event

import model.FlightDataModel

sealed class FlightEvent {
    data class Saved(val flight: FlightDataModel, val isNew: Boolean) : FlightEvent()
    data class Failed(val flight: FlightDataModel, val exception: Throwable) : FlightEvent()
}