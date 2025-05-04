package repository

import model.AirlinesLightModel
import model.FlightDataModel
import model.FlightTracksModel

class FlightDataRepository {
    suspend fun getAirlineByIcao(icao: String): AirlinesLightModel? {
        // Query Supabase / Database
        return null
    }

    suspend fun getFlightByNumber(flight: String): FlightDataModel? {
        // Query Supabase / Database
        return null
    }

    suspend fun saveAirline(airline: AirlinesLightModel) {
        // Insert into Supabase / Database
    }

    suspend fun saveFlight(flight: FlightDataModel) {
        // Insert into Supabase / Database
    }

    fun getTrack(id: String): FlightTracksModel? {
        // Query Supabase / Database
        return null
    }

    fun saveTrack(it: FlightTracksModel) {

    }
}