package util

import kotlinx.serialization.Serializable
import model.FlightSummaryModel

@Serializable
sealed class FlightRequest : FlightRequestValidator {
    abstract override fun validate(): List<String>
    override fun isValidCallsign(callsign: String): Boolean =
        callsign.split(",").all { it.trim().matches(FlightRequestValidator.CALLSIGN_PATTERN) }
    override fun isValidIcao(icao: String): Boolean = icao.matches(FlightRequestValidator.ICAO_PATTERN)
    override fun isValidFlightId(flightId: String): Boolean = flightId.matches(FlightRequestValidator.FR24ID_PATTERN)
    override fun isValidAirlineIcao(icao: String): Boolean = icao.matches(FlightRequestValidator.AIRLINE_ICAO_PATTERN)
    override fun isValidDatetime(datetime: String): Boolean {
        return datetime.matches(FlightRequestValidator.DATETIME_PATTERN)
    }


    @Serializable
    data class Airline(
        val icao: String,
    ) : FlightRequest() {
        override fun validate(): List<String> {
            val errors = mutableListOf<String>()
            if (!isValidAirlineIcao(icao)) {
                errors.add("Invalid Airline Icao: '$icao'")
            }
            return errors
        }
    }

    @Serializable
    data class Summary(
        val callsign: String,
        val datetimeFrom: String,
        val datetimeTo: String
    ) : FlightRequest() {
        override fun validate(): List<String> {
            val errors = mutableListOf<String>()

            if (!isValidCallsign(callsign)) {
                errors.add("Invalid Callsign: '$callsign'")
            }

            if (!isValidDatetime(datetimeFrom)) {
                errors.add("Invalid datetimeFrom: '$datetimeFrom'")
            }

            if (!isValidDatetime(datetimeTo)) {
                errors.add("Invalid datetimeTo: '$datetimeTo'")
            }

            return errors
        }
    }

    @Serializable
    data class Save(
        val flights: List<FlightSummaryModel>,
    ): FlightRequest() {
        override fun validate(): List<String> {
            val errors = mutableListOf<String>()

            // TODO: add flight entity validation if list is not empty

            if (flights.isEmpty()) {
                errors.add("Flights list cannot be empty")
            }

            return errors
        }
    }

    @Serializable
    data object GetAllFlights : FlightRequest() {
        override fun validate(): List<String> = emptyList()
    }

    @Serializable
    data object GetAllTracks : FlightRequest() {
        override fun validate(): List<String> = emptyList()
    }

    @Serializable
    class Track(
        val flightId: String
    ) : FlightRequest() {
        override fun validate(): List<String> {
            val errors = mutableListOf<String>()
            if (flightId.isEmpty()) {
                errors.add("Flight ID cannot be empty")
            }
            if (!isValidFlightId(flightId)) {
                errors.add("Invalid Flight ID: '$flightId'")
            }
            return errors
        }
    }
}
