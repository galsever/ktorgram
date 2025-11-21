package org.srino.modules

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.srino.routes.PostRoutes

fun Application.configureRouting() {

    routing {
        PostRoutes().apply {
            routes()
        }
    }


}

interface Routes {
    fun Routing.routes()
}