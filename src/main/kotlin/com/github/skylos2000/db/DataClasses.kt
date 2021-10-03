package com.github.skylos2000.db

import kotlinx.serialization.Serializable

@Serializable
data class RowUser(
    val id: Int,
    val email: String,
    val username: String,
    val password: String, // TODO: Figure out secure password storage
    val defaultPickupLatitude: Double?,
    val defaultPickupLongitude: Double?,
)

// Route itself will be a list of these
@Serializable
data class RowRouteStopPoint(val latitude: Double, val longitude: Double)

@Serializable
data class RowDestination(val label: String, val longitude: Double, val latitude: Double)

@Serializable
data class Group(
    val id: Int,
    val label: String,
    val isTemp: Boolean,
    val leader: Int,
    // TODO: Reimplement these
    // val route: List<RowRouteStopPoint>,
    // val members: List<Int>,
    // val destinations: List<RowDestination>
)

