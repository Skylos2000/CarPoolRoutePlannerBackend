package com.github.skylos2000

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.github.skylos2000.plugins.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object User1 : Table() {
    val Uid: Column<Int> = integer("Uid")
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

fun main() {
    val mydb = Database.connect("jdbc:mysql://localhost:3306/mydb?useSSL=false", driver = "com.mysql.jdbc.Driver",
        user = "bigBoi", password = "bigBoi")

    transaction(mydb) {
        SchemaUtils.create(User1,Routes,Group1,Group_Membership,Group_Destinations)
    }

    val get_Uid = transaction(mydb) {
        User1.slice(User1.Uid).selectAll().toList()
    }

    transaction {

        User1.insert {
            it[Uid] = 999
            it[Email] = "999"
            it[Username] = "999"
            it[Password] = "999"
            it[Default_Pickup_Lat] = 42.0
            it[Default_Pickup_Long] = 42.0
        }

        Routes.insert {
            it[Rid] = 999
            it[Destination_lat] = 33.00
            it[Destination_long] = 33.00
            it[Order1] = 5
        }

        Group1.insert {
            it[Group_ID] = 888
            it[isTemp] = false
            it[group_leader] = 999
            it[Route_id] = 1
        }

        Group_Membership.insert {
            it[Gid] = 888
            it[Uid] = 999
            it[User_Lat] = 23.00
            it[User_Long] = 44.00
        }

        Group_Destinations.insert {
            it[Group_id] = 888
            it[Destination_Lat] = 35.66
            it[Destination_Long] = 44.55
            it[Label] = "somewhere"
        }
    }

    val x = transaction {
        User1.selectAll().toList()
    }

    /*println(userInfo)
    println(get_Uid)
    */

    println(x)

/*
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureSecurity()
        configureSerialization()
    }.start(wait = true)
*/

}
