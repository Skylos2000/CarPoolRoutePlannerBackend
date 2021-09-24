package com.github.skylos2000


import com.github.skylos2000.db.User1
import com.github.skylos2000.db.getUserFromResultRow
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.server.testing.*
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.config.*
import io.netty.handler.codec.http.HttpMethod.POST
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertEquals
import org.junit.Test
//import sun.awt.www.content.audio.basic
import javax.swing.text.AbstractDocument
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.server.testing.*
import kotlin.test.*

// Source: https://github.com/ktorio/ktor/blob/main/ktor-features/ktor-auth/jvm/test/io/ktor/tests/auth/OAuth2.kt
@InternalAPI // TODO: Figure out how to encode into base64 without using internal APIs
private fun TestApplicationEngine.handleRequestWithBasic(url: String, user: String, pass: String,
                                                         method: HttpMethod = HttpMethod.Get,
                                                         handler: (TestApplicationRequest.() -> Unit) = {}) =
    handleRequest(method, url) {
        val up = "$user:$pass"
        val encoded = up.toByteArray(Charsets.ISO_8859_1).encodeBase64()
        addHeader(HttpHeaders.Authorization, "Basic $encoded")

        this.handler()
    }


@InternalAPI
class ApplicationTest {

    val user = "aaa"
    val pass = "eee"

    private val testEnv = createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }
    @Test
    fun testRoot() {
        withApplication(testEnv) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Hello World!", response.content)
            }
        }
    }

    @Test
    fun testUserLogIn() {
        withApplication(testEnv) {
            handleRequestWithBasic("/example/what_is_my_name/", user, pass).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("aaa", response.content)
            }
            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
        }
    } //postman

    @Test
    fun testUserPickup() {
        withApplication(testEnv) {
            handleRequestWithBasic("/example/my_pickup_coords", user, pass).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("32.525578, -92.6444764", response.content)
            }
            //call.respondText( loggedInUser.defaultPickupLatitude.toString() + ", " +
            //                  loggedInUser.defaultPickupLongitude.toString() )
        }
    }

    @Test
    fun testUserInfo() {
        withApplication(testEnv) {
            handleRequestWithBasic("/get_user_info/2", user, pass).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val userRow = getUserFromResultRow(
                    transaction() {
                        User1.select { User1.Uid eq 2 }.single()
                    })

                val userRowJson = Json.encodeToString(userRow.copy(password = ""))

                assertEquals(userRowJson, response.content)
            }
            //call.respondText( loggedInUser.defaultPickupLatitude.toString() + ", " +
            //                  loggedInUser.defaultPickupLongitude.toString() )
        }
    }


    @Test
    fun testListMyGroups() {
        withApplication(testEnv) {
            handleRequestWithBasic("/list_my_groups/", user, pass).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("[456]", response.content)
            }
        }
    }

    // what is this route even supposed to return??
    @Test
    fun testGetGroupRoutes() {
        withApplication(testEnv) {
            handleRequestWithBasic("/get_group_routes/789", user, pass).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                //assertEquals("100.0,120.0", response.content)
            }
            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
        }
    }

    @Test
    fun testGetGroupMembersLocation() {
        withApplication(testEnv) {
            handleRequestWithBasic("/get_group_members/456", user, pass).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("[2]", response.content)
            }
            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
        }
    }

    @Test
    fun testSetGroupDestinationLocation() {
        withApplication(testEnv) {
            handleRequestWithBasic("/set_group_destination/456?=newLat=0&newLong=0&label=a", user, pass).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("[2,4]", response.content)
            }
            /*
            Group_Destinations.deleteWhere { Group_Destinations.Group_id eq pathParams.groupId }
                    Group_Destinations.insert {
                        it[Group_id] = pathParams.groupId
                        it[Destination_Lat] = pathParams.newLat
                        it[Destination_Long] = pathParams.newLong
                        it[Label] = pathParams.label
                    }
             */
        }
    }

    @Test
    fun testSubmitLocation() {
        withApplication(testEnv) {
            handleRequestWithBasic("/submit_location", user, pass, HttpMethod.Post){
                setBody("456,99,99,1,home")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Success", response.content)
            }
            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
        }
    }

    @Test
    fun testCreateGroup() {
        withApplication(testEnv) {
            handleRequestWithBasic("/set_my_pickup_location", user, pass).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Group created", response.content)
            }
            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
        }
    }

    @Test
    fun testStartVote() {
        withApplication(testEnv) {
            handleRequestWithBasic("/startVote", user, pass, HttpMethod.Post) {
                setBody("456")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Vote started", response.content)
            }
        }
    }

    @Test
    fun testCastVote() {
        withApplication(testEnv) {
            handleRequestWithBasic("/castVote", user, pass, HttpMethod.Post) {
                setBody("456,taco bell")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Vote has been cast", response.content)
            }
        }
    }

    @Test
    fun testAddVotingLocation() {
        withApplication(testEnv) {
            handleRequestWithBasic("/addVotingLocation", user, pass, HttpMethod.Post) {
                setBody("456,taco bell")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Voting location added", response.content)
            }
        }
    }

    @Test
    fun testVotingOptions() {
        withApplication(testEnv) {
            handleRequestWithBasic("/votingOptions", user, pass, HttpMethod.Post) {
                setBody("456")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun testVotingScores() {
        withApplication(testEnv) {
            handleRequestWithBasic("/votingScores", user, pass, HttpMethod.Post) {
                setBody("456")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun testVoteResult() {
        withApplication(testEnv) {
            handleRequestWithBasic("/voteResult", user, pass, HttpMethod.Post) {
                setBody("456")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Voting ended", response.content)
            }
        }
    }

}