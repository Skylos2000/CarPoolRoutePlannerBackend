package com.github.skylos2000.routes

import com.github.skylos2000.db.Group1
import com.github.skylos2000.db.Polls
import com.github.skylos2000.plugins.getLoggedInUser
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.initVotingRoutes(db: Database) {
    routing {
        authenticate("auth-basic") {
            post("/startVote") {
                val gid = call.receiveText()
                val uid = call.getLoggedInUser()?.id

                val groupLeader = transaction(db) {
                    Group1.select { Group1.Group_ID eq gid.toInt() }.map { it[Group1.group_leader] }
                }

                if (uid == groupLeader[0]) {
                    transaction(db) {
                        Group1.update({ Group1.Group_ID eq gid.toInt() }) {
                            it[isVoting] = true
                        }
                    }

                    call.respondText("Vote started")
                } else {
                    call.respondText("-2")
                }
            }

            post("/addVotingLocation") {
                val (gid, dest) = call.receiveText().split(",")

                val voteStartCheck = transaction(db) {
                    Group1.select { Group1.Group_ID eq gid.toInt() }.map { it[Group1.isVoting] }
                }

                //println(aaa[0])

                if (voteStartCheck[0]) {
                    transaction(db) {
                        Polls.insert {
                            it[Group_id] = gid.toInt()
                            it[Location] = dest
                            it[Votes] = 0
                        }
                    }

                    call.respondText("Voting location added")
                }
                //else{ call.respondText("There is no active no voting for this group") }
                else {
                    call.respondText("-1")
                }
            }

            post("/castVote") {
                val (gid, dest) = call.receiveText().split(",")

                val voteStartCheck = transaction(db) {
                    Group1.select { Group1.Group_ID eq gid.toInt() }.map { it[Group1.isVoting] }
                }

                if (voteStartCheck[0]) {
                    transaction(db) {
                        val currVotes = Polls.select { (Polls.Group_id eq gid.toInt()) and (Polls.Location eq dest) }
                            .map { it[Polls.Votes] }
                        println(currVotes)

                        Polls.update({ (Polls.Group_id eq gid.toInt()) and (Polls.Location eq dest) }) {
                            it[Votes] = currVotes[0] + 1

                        }
                    }

                    call.respondText("Vote has been cast")
                } else {
                    call.respondText("-1")
                }
            }

            post("/voteResult") {
                val gid = call.receiveText()
                val uid = call.getLoggedInUser()?.id

                val groupLeader = transaction(db) {
                    Group1.select { Group1.Group_ID eq gid.toInt() }.map { it[Group1.group_leader] }
                }

                if (uid == groupLeader[0]) {

                    val voteStartCheck = transaction(db) {
                        Group1.select { Group1.Group_ID eq gid.toInt() }.map { it[Group1.isVoting] }
                    }

                    if (voteStartCheck[0]) {
                        transaction(db) {
                            // sets group voting back to false
                            Group1.update({ Group1.Group_ID eq gid.toInt() }) {
                                it[isVoting] = false
                            }

                            // deletes the voting locations
                            Polls.deleteWhere { Polls.Group_id eq gid.toInt() }
                        }
                        call.respondText("Voting ended")
                    } else {
                        call.respondText("-1")
                    }
                } else {
                    call.respondText("-2")
                }
            }

            post("/votingOptions") {
                val gid = call.receiveText()

                val voteStartCheck = transaction(db) {
                    Group1.select { Group1.Group_ID eq gid.toInt() }.map { it[Group1.isVoting] }
                }

                if (voteStartCheck[0]) {
                    call.respond(
                        transaction(db) {
                            Polls.select { Polls.Group_id eq gid.toInt() }.map { it[Polls.Location] }
                        }
                    )
                } else {
                    call.respondText("-1")
                }

            }

            post("/votingScores") {
                val gid = call.receiveText()

                val voteStartCheck = transaction(db) {
                    Group1.select { Group1.Group_ID eq gid.toInt() }.map { it[Group1.isVoting] }
                }

                if (voteStartCheck[0]) {
                    call.respond(
                        transaction(db) {
                            //Polls.select { Polls.Group_id eq gid.toInt() }.map { listOf(it[Polls.Location], it[Polls.Votes].toString()) }
                            Polls.select { Polls.Group_id eq gid.toInt() }
                                .map { it[Polls.Location] + ": " + it[Polls.Votes].toString() }
                        }
                    )
                } else {
                    call.respondText("-1")
                }
            }
        }
    }
}
