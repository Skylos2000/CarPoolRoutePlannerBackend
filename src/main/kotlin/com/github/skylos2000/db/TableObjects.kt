package com.github.skylos2000.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object User1 : Table() {
    val Uid: Column<Int> = integer("Uid").autoIncrement()
    val Email: Column<String> = varchar("Email", 45)
    val Username: Column<String> = varchar("Username", 45)
    val Password: Column<String> = varchar("Password", 45)
    val Default_Pickup_Lat: Column<Double> = double("Default_Pickup_Lat")
    val Default_Pickup_Long: Column<Double> = double("Default_Pickup_Long")

    override val primaryKey = PrimaryKey(Uid)
}

object Routes : Table(){
    val Rid: Column<Int> = integer("Rid")
    val Destination_lat: Column<Double> = double("Destination_lat")
    val Destination_long: Column<Double> = double("Destination_long")
    val Order1 : Column<Int> = integer("Order1")

    override val primaryKey = PrimaryKey(Rid, Order1)
}

object Group1 : Table(){
    val Group_ID: Column<Int> = integer("Group_ID")
    val isTemp: Column<Boolean> = bool("isTemp")
    val group_leader = integer("group_leader").uniqueIndex().references(User1.Uid)
    val Route_id = integer("Route_id").uniqueIndex().references(Routes.Rid)

    override val primaryKey = PrimaryKey(Group_ID)
}

object  Group_Membership : Table() {
    val Gid = reference("Gid", Group1.Group_ID).uniqueIndex()
    val Uid = reference("Uid", User1.Uid).uniqueIndex()
    val User_Lat: Column<Double> = double("User_Lat")
    val User_Long: Column<Double> = double("User_Long")

    override val primaryKey = PrimaryKey(Gid, Uid)
}

object  Group_Destinations : Table() {
    val Group_id = reference("Group_id", Group1.Group_ID).uniqueIndex()
    val Destination_Lat: Column<Double> = double("Destination_Lat")
    val Destination_Long: Column<Double> = double("Destination_Long")
    val Label: Column<String> = varchar("Label", 45)

    override val primaryKey = PrimaryKey(Group_id, Destination_Lat, Destination_Long)
}