package com.github.skylos2000

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json as JsonSerializer

val httpClient = HttpClient {
    install(JsonFeature) {

        // This is needed to ignore all the extra keys in the response from OSRM
        serializer = KotlinxSerializer(JsonSerializer {
            ignoreUnknownKeys = true
        })
    }
}

// Temporary Locations for testing purposes
//val locations = mapOf<String, Pair<Double, Double>>(
//    "Dairy Queen" to Pair(32.54386464788708, -92.65289852371741),
//    "Canes" to Pair(32.54107667280471, -92.6294690516874),
//    "Chick-Fil-A" to Pair(32.54105472946106, -92.63309141687111),
//    "Walmart" to Pair(32.542931056568044, -92.62541758069071),
//    "Tractor Supply" to Pair(32.54082962662338, -92.60580263124608),
//    "Starbucks" to Pair(32.538904834904436, -92.6528500849125)
//)

@Serializable
data class OSRMTripResponse(val code: String, val waypoints: List<Waypoint>) {
    @Serializable
    data class Waypoint(
        val waypoint_index: Int,
        val trips_index: Int,
        val location: List<Double>,
        val distance: Double
    )
}

// Take any number of pairs of doubles and return them in the format OSRM wants them in.
fun coordinateList(points: List<GroupDestination>) = points.joinToString(";") { "${it.long},${it.lat}" }

suspend fun Application.getTripService(points: List<GroupDestination>): List<GroupDestination> {
    // Create the URL to request the distance matrix from the OSRM backend
    // Documentation here: http://project-osrm.org/docs/v5.5.1/api/?language=cURL#table-service
    val backendHost = environment.config.property("osrm.routed_host").getString()
    val tableServicePath = "/trip/v1/car/${coordinateList(points)}"
    val tripOptions = "?roundtrip=false&source=first&destination=last"

    // Combine the pieces together
    val urlString = backendHost + tableServicePath + tripOptions

    // Get the table from the OSRM backend
    val tableResponse = httpClient.get<OSRMTripResponse>(urlString)

    // Pair is intentionally reversed to deal with OSRM's format
//    return tableResponse.waypoints.sortedBy { it.waypoint_index }.map { Pair(it.location[1], it.location[0]) }
    return tableResponse.waypoints.mapIndexed { index, waypoint -> points[index].copy(orderNum = waypoint.waypoint_index) }
}
