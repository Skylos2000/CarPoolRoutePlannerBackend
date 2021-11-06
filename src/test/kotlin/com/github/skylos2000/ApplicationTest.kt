package com.github.skylos2000


//import sun.awt.www.content.audio.basic
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
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

private fun TestApplicationEngine.handleRequestWithJWT(url: String, user: String, pass: String,
                                                         method: HttpMethod = HttpMethod.Get,
                                                         handler: (TestApplicationRequest.() -> Unit) = {}) =
    handleRequest(method, url) {
        addHeader(HttpHeaders.Authorization, hashMapOf("username" to user, "password" to pass).toString())
        this.handler()
    }

@InternalAPI
class ApplicationTest {

    val url = "http://192.168.1.133:8080/"

    val user = "dev1"
    val pass = "1"

    lateinit var token: String
    val expectedToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwOi8vMC4wLjAuMDo4MDgwL2hlbGxvIiwiaXNzIjoiaHR0cDovLzAuMC4wLjA6ODA4MC8iLCJ1c2VybmFtZSI6ImRldjEifQ.gzFXVrHW0sSs4rdFkEdrr0Ob2n3s79f3HrSdha9zDow"
    //lateinit var res: HttpResponse

    private val testEnv = createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }

    val loginClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer() // TODO: Is this needed?
        }
        install(Auth){
            bearer {
                loadTokens {
                    BearerTokens(expectedToken, "")
                }
            }
        }
    }

    @Test
    fun testInitialLogin(){
        withApplication(testEnv){

            runBlocking {
                token = loginClient.post<String>(url + "login") {
                    contentType(ContentType.Application.Json)
                    body = hashMapOf("username" to user, "password" to pass)
                }
            }.apply {
                assertEquals(expectedToken, token)
            }
            print("fref")
        }
    }

    @Test
    fun testUserLogIn() {
        withApplication(testEnv) {
            var response = ""
            runBlocking {
                    response = loginClient.get<String>(url + "users/me")
            }.apply {
                //assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("{\"userId\":1,\"email\":\"qwe.gmail.com\",\"username\":\"dev1\",\"groupIds\":[123]}", response)
            }
            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
        }
    } //postman

    // what is this route even supposed to return??
    // TODO: find out what this route is supposed to return
    @Test
    fun testGetGroupRoutes() {
        withApplication(testEnv) {
            var response = ""
            runBlocking {
                response = loginClient.get(url + "get_group_routes/789")
            }.apply {
                assertEquals(HttpStatusCode.OK, response)
                //assertEquals("100.0,120.0", response.content)
            }
            //call.respondText(call.getLoggedInUser()?.username ?: "no one")
        }
    }

    @Test
    fun testGetGroupMembersLocation() {
        withApplication(testEnv) {
            var response = ""
            runBlocking {
                response = loginClient.get(url + "get_group_members/456")
            }.apply {
                //assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("[2,4]", response)
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
            var response = ""
            runBlocking {
                response = loginClient.post(url + "startVote"){
                    body = "456"
                }
            }.apply {
                //assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("-2", response)
            }
        }
    }

    @Test
    fun testCastVote() {
        withApplication(testEnv) {
            var response = ""
            runBlocking {
                response = loginClient.post(url + "castVote"){
                    body = "456,taco bell"
                }
            }.apply {
                //assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("-1", response)
            }
        }
    }

    @Test
    fun testAddVotingLocation() {
        withApplication(testEnv) {
            var response = ""
            runBlocking {
                response = loginClient.post(url + "addVotingLocation"){
                    body = "456,taco bell"
                }
            }.apply {
                //assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("-1", response)
            }
        }
    }

    @Test
    fun testVotingOptions() {
        withApplication(testEnv) {
            var response = ""
            runBlocking {
                response = loginClient.post(url + "votingOptions"){
                    body = "456"
                }
            }.apply {
                assertEquals("-1", response)
            }
        }
    }

    @Test
    fun testVotingScores() {
        withApplication(testEnv) {
            var response = ""
            runBlocking {
                response = loginClient.post(url + "votingScores"){
                    body = "456"
                }
            }.apply {
                assertEquals("-1", response)
            }
        }
    }

    @Test
    fun testVoteResult() {
        withApplication(testEnv) {
            var response = ""
            runBlocking {
                response = loginClient.post(url + "voteResult"){
                    body = "456"
                }
            }.apply {
                //assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("-2", response)
            }
        }
    }

}