package org.srino.modules

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import org.srino.dotenv
import org.srino.logic.Session
import org.srino.logic.User
import org.srino.managers.GsonSessionSerializer
import org.srino.sessionManager
import org.srino.sharedGet
import org.srino.user
import org.srino.userManager
import java.util.*

data class UserSession(val sessionId: String)

fun Application.configureSecurity() {

    val httpClient = HttpClient(Apache) {
        install(ContentNegotiation) {
            gson()
        }
    }

    val signingKey = hex(dotenv["AUTH_SIGNING_KEY"])
    val encryptKey = hex(dotenv["AUTH_ENCRYPTION_KEY"])

    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.httpOnly = true
            cookie.extensions["SameSite"] = "lax"

            serializer = GsonSessionSerializer(UserSession::class.java)
            transform(SessionTransportTransformerEncrypt(encryptKey, signingKey))
        }
    }

    authentication {
        oauth("auth-oauth-discord") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "discord",
                    authorizeUrl = "https://discord.com/api/oauth2/authorize",
                    accessTokenUrl = "https://discord.com/api/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = dotenv["DISCORD_CLIENT_ID"],
                    clientSecret = dotenv["DISCORD_CLIENT_SECRET"],
                    defaultScopes = listOf("email", "identify"),
                )
            }
            client = httpClient
        }
        session<UserSession>("session-auth") {
            validate { session ->
                val storedSession = sessionManager[session.sessionId] ?: return@validate null
                userManager[storedSession.userId] ?: return@validate null
                val isExpired = storedSession.expiresAt < System.currentTimeMillis()
                if (isExpired) {
                    sessionManager.delete(storedSession.id)
                    return@validate null
                }
                return@validate storedSession
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, "You are not logged in")
            }
        }
    }

    routing {

        authenticate("auth-oauth-discord") {
            get("/login") {
                // Redirects to 'authorizeUrl' automatically
            }

            get("/callback") {
                val currentPrincipal: OAuthAccessTokenResponse.OAuth2 = call.principal() ?: run {
                    call.respondText("An error occurred while authenticating")
                    return@get
                }
                val user = httpClient.get("https://discord.com/api/users/@me") {
                    bearerAuth(currentPrincipal.accessToken)
                }.body<User>()

                userManager[user.id] = user

                val session = Session(
                    UUID.randomUUID().toString(),
                    user.id,
                )

                sessionManager[session.id] = session

                val userSession = UserSession(session.id)
                call.sessions.set(userSession)

                call.respondRedirect("http://localhost:5173/app")
            }
        }

        authenticate("session-auth") {
            sharedGet<User>("/auth/me", "currentUser") {

                val user = user() ?: return@sharedGet
                call.respond(user)

            }

            get("/logout") {
                val userSession = call.sessions.get<UserSession>() ?: return@get
                sessionManager.delete(userSession.sessionId)
                call.sessions.clear<UserSession>()
                call.respondRedirect("http://localhost:5173/")
            }
        }
    }
}
