
package com.github.skylos2000.db

import io.ktor.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun getUserFromResultRow(resultRow: ResultRow): RowUser = RowUser(
    resultRow[User1.Uid],
    resultRow[User1.Email],
    resultRow[User1.Username],
    resultRow[User1.Password],
    resultRow[User1.Default_Pickup_Lat],
    resultRow[User1.Default_Pickup_Long]
)

fun Application.initDb(): Database {
    val db = Database.connect(
        environment.config.property("db.url").getString(),
        driver = environment.config.property("db.driver").getString(),
        user = environment.config.property("db.user").getString(),
        password = environment.config.property("db.password").getString()
    )

    transaction(db) {
        SchemaUtils.create(User1, Routes, Group1, Group_Membership, Group_Destinations, GroupInvites)
    }

    return db
}