package util

interface FlightRequestValidator {
    /**
     * Validates the request and returns a list of validation errors.
     * An empty list indicates that the request is valid.
     *
     * @return List of validation error messages, empty if valid
     */
    fun validate(): List<String>

    /**
     * Checks if a callsign matches the required pattern.
     *
     * @param callsign The callsign to validate
     * @return true if valid, false otherwise
     */
    fun isValidCallsign(callsign: String): Boolean

    /**
     * Checks if an ICAO code matches the required pattern.
     *
     * @param icao The ICAO code to validate
     * @return true if valid, false otherwise
     */
    fun isValidIcao(icao: String): Boolean

    /**
     * Checks if an airline ICAO code matches the required pattern.
     *
     * @param icao The airline ICAO code to validate
     * @return true if valid, false otherwise
     */
    fun isValidAirlineIcao(icao: String): Boolean

    /**
     * Checks if a datetime string matches the required pattern.
     *
     * @param datetime The datetime string to validate
     * @return true if valid, false otherwise
     */
    fun isValidDatetime(datetime: String): Boolean

    /**
     * Validates whether the given flight ID matches the required pattern.
     *
     * @param flightId The flight ID to validate
     * @return true if the flight ID is valid, false otherwise
     */
    fun isValidFlightId(flightId: String): Boolean

    companion object {
        val FR24ID_PATTERN = Regex("^[a-z\\d]{8}$")
        val CALLSIGN_PATTERN = Regex("^[A-Za-z0-9]+$")
        val ICAO_PATTERN = Regex("^[A-Z]{4}$")
        val AIRLINE_ICAO_PATTERN = Regex("^[A-Z]{3}$")
        val DATETIME_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2}Z)?\$")
    }
}