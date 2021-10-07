package com.github.skylos2000


//import sun.awt.www.content.audio.basic
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import org.junit.Assert.assertEquals
import org.junit.Test

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

    val user = "dev1"
    val pass = "1"

    private val testEnv = createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }
    @Test
    fun testUserLogIn() {
        withApplication(testEnv) {
            handleRequestWithBasic("/users/me", user, pass).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("{\"userId\":1,\"email\":\"qwe.gmail.com\",\"username\":\"dev1\",\"groupIds\":[123]}", response.content)
            }
            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
        }
    } //postman

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
                assertEquals("[2,4]", response.content)
            }
            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
        }
    }

  /*  @Test
    fun testSetGroupDestinationLocation() {
        withApplication(testEnv) {
            handleRequestWithBasic("/set_group_destination/456?=newLat=0.0&newLong=0.0&label=\"a\"", user, pass).apply {
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
    }*/

//    @Test
//    fun testSubmitLocation() {
//        withApplication(testEnv) {
//            handleRequestWithBasic("/submit_location", user, pass, HttpMethod.Post){
//                setBody("456,99,99,1,home")
//            }.apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                assertEquals("Success", response.content)
//            }
//            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
//        }
//    }
//
//    @Test
//    fun testCreateGroup() {
//        withApplication(testEnv) {
//            handleRequestWithBasic("/set_my_pickup_location", user, pass).apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                assertEquals("Group created", response.content)
//            }
//            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
//        }
//    }

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