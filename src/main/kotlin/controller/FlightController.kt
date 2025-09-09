package controller

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import service.FlightService
import io.ktor.util.logging.*
import kotlinx.coroutines.withContext
import plugin.UserIdContext
import plugin.authenticationPlugin
import plugin.getUserId
import util.FlightRequest

/**
 * Configures the routing for flight-related endpoints.
 *
 * This method defines several routes under the `/flights` path for managing flight data.
 * The routes include endpoints for fetching airline flights by ICAO code, retrieving flight summaries,
 * saving flight summaries, and fetching all flights for a user. It also installs an authentication plugin
 * to ensure that requests contain valid user tokens.
 *
 * @param flightService An instance of [FlightService] that provides business logic for handling flight-related operations.
 * @param logger A [Logger] used for logging information and errors during request handling.
 */
fun Route.flightRoutes(flightService: FlightService, logger: Logger) {
    logger.info("Setting up flight routes")
    route("/flights") {
        install(authenticationPlugin())

        get("/airline/{icao}") {
            handleFlightRequest(
                context = this,
                logger = logger,
                requestName = "Airline Flights",
                getRequest = { call.receive<FlightRequest.Airline>() },
                processRequest = { request -> flightService.getAirlineLight(request) },
                notFoundMessage = "Airline not found"
            )
        }

        // Flight summary endpoint
        post("/summary") {
            handleFlightRequest(
                context = this,
                logger = logger,
                requestName = "Flight Summary",
                getRequest = { call.receive<FlightRequest.Summary>() },
                processRequest = { request -> flightService.getFlightSummary(request) },
                notFoundMessage = "Flight summary not found"
            )
        }

        // Save flight endpoint
        post("/save") {
            handleFlightRequest(
                context = this,
                logger = logger,
                requestName = "Save Flight Summary",
                getRequest = { call.receive<FlightRequest.Save>() },
                processRequest = { request -> flightService.saveFlights(request) },
                notFoundMessage = "Could not save flight summary"
            )
        }

        // Get all flights endpoint
        get("/allFlights") {
            handleFlightRequest(
                context = this,
                logger = logger,
                requestName = "All Flights",
                getRequest = { FlightRequest.GetAllFlights },
                processRequest = { _ -> flightService.getAllFlights() },
                notFoundMessage = "No flights found"
            )
        }

        get("/allTracks") {
            handleFlightRequest(
                context = this,
                logger = logger,
                requestName = "All Tracks",
                getRequest = { FlightRequest.GetAllTracks },
                processRequest = { _ -> flightService.getAllFlightTracks() },
                notFoundMessage = "No tracks found"
            )
        }
    }
}

/**
 * Handles the processing of a flight request. It validates the request, logs relevant information, executes the
 * processing logic, and sends an appropriate response back to the client.
 *
 * @param T The type of the flight request, which must extend [FlightRequest].
 * @param R The type of the response object returned by the processing logic.
 * @param context The [RoutingContext] used to access the application call and respond to the client.
 * @param logger The logger used to log messages and errors during request processing.
 * @param requestName A string representing the name of the request, used for logging purposes.
 * @param getRequest A lambda function that provides an instance of the flight request.
 * @param processRequest A lambda function that processes the validated flight request and returns a response or null.
 *   The function takes the flight request and the user ID as parameters.
 * @param notFoundMessage The error message returned to the client when the result of the request processing is null.
 */
private suspend inline fun <reified T : FlightRequest, reified R> handleFlightRequest(
    context: RoutingContext,
    logger: Logger,
    requestName: String,
    getRequest: () -> T,
    crossinline processRequest: suspend (T) -> R?,
    notFoundMessage: String
) {
    val userId = context.call.getUserId()
    val request = getRequest()
    val validationErrors = request.validate()
    if (validationErrors.isNotEmpty()) {
        return context.call.respond(HttpStatusCode.BadRequest, mapOf("error" to validationErrors))
    }

    try {
        logger.info("Processing $requestName request")

        val response = withContext(context.call.coroutineContext + UserIdContext(userId)) {
            processRequest(request)
        }

        if (response != null) {
            context.call.respond(response)
            logger.info("$requestName request completed successfully")
        } else {
            context.call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to notFoundMessage)
            )
            logger.info("$requestName request resulted in not found")
        }
    } catch (e: Exception) {
        logger.error("Error processing $requestName request: ${e.message}", e)
        context.call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to (e.localizedMessage ?: "Unknown error"))
        )
    }
}