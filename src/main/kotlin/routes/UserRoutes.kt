package org.srino.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import org.srino.modules.Routes
import org.srino.userManager

class UserRoutes: Routes {
    override fun Routing.routes() { authenticate("session-auth") {
        get("/user/{name}") {
            val name = call.parameters["name"] ?: return@get
            val user = userManager.usersByUsername[name] ?: return@get
            call.respond(user)
        }
    } }
}