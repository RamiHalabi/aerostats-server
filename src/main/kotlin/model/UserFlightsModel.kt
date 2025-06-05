package model

class UserFlightsModel(
    val user_id: String,
    val flight_id: String,
    val callsign: String,
    val origIcao: String,
    val destIcao: String,
    val date: String,
) {
    override fun toString(): String {
        return "UserFlightsModel(user_id='$user_id', flight_id='$flight_id', callsign='$callsign', origIcao='$origIcao', destIcao='$destIcao', date='$date')"
    }
}