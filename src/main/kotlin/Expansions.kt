package org.srino

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.srino.logic.User
import org.srino.modules.UserSession
import kotlin.time.Duration

fun CoroutineScope.every(period: Duration, block: suspend () -> Unit) = launch {
    delay(period)
    while (isActive) {
        block()
        delay(period)
    }
}

fun Application.onStop(block: suspend () -> Unit) {
    monitor.subscribe(ApplicationStopping) {
        runBlocking {
            block()
        }
    }
}

suspend fun RoutingContext.user(): User? {
    val userSession = call.sessions.get<UserSession>() ?: return null

    val session = sessionManager[userSession.sessionId] ?: run {
        call.respond(HttpStatusCode.Unauthorized, "Your session does not exist")
        return null
    }

    val user = userManager[session.userId] ?: run {
        call.respond(HttpStatusCode.Unauthorized, "Your session does not exist")
        return null
    }
    return user
}