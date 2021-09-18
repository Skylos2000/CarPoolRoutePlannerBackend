package com.github.skylos2000.db

import com.github.skylos2000.db.Group_Destinations.uniqueIndex
import com.github.skylos2000.db.Group_Membership.uniqueIndex
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import javax.xml.stream.Location

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
    val isVoting: Column<Boolean> = bool("isVoting")
    val group_leader = integer("group_leader").uniqueIndex().references(User1.Uid)
    val Route_id = integer("Route_id").uniqueIndex().references(Routes.Rid)

    override val primaryKey = PrimaryKey(Group_ID)
}

object  Group_Membership : Table() {
    val Gid = reference("Gid", Group1.Group_ID).uniqueIndex()
    val Uid = reference("Uid", User1.Uid).uniqueIndex()
    val User_Lat: Column<Double?> = double("User_Lat").nullable()
    val User_Long: Column<Double?> = double("User_Long").nullable()

    override val primaryKey = PrimaryKey(Gid, Uid)
}

object  Group_Destinations : Table() {
    val Group_id = reference("Group_id", Group1.Group_ID).uniqueIndex()
    val Destination_Lat: Column<Double> = double("Destination_Lat")
    val Destination_Long: Column<Double> = double("Destination_Long")
    val isPriority: Column<Boolean> = bool("isPriority")
    val Label: Column<String> = varchar("Label", 45)

    override val primaryKey = PrimaryKey(Group_id, Destination_Lat, Destination_Long)
}

object GroupInvites: Table() {
    val id : Column<Int> = integer("id").autoIncrement()
    val Gid = reference("Gid", Group1.Group_ID).uniqueIndex()
    val InviteId : Column<String> = varchar("Invite_Id",15)

    override val primaryKey = PrimaryKey(
       id
    )

}

object Polls : Table() {
    val Poll_id: Column<Int> = integer("Poll_id").autoIncrement()
    val Group_id = reference("Group_id", Group1.Group_ID)
    val Location: Column<String> = varchar("Location", 45)
    val Votes: Column<Int> = integer("Votes")

    override val primaryKey = PrimaryKey(Poll_id)
}