package org.srino

import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import org.srino.database.Database
import org.srino.debug.Debug
import org.srino.ktorupload.KtorUpload
import org.srino.managers.GsonManager
import org.srino.managers.PostManager
import org.srino.managers.SessionManager
import org.srino.managers.UserManager
import org.srino.modules.configureRouting
import org.srino.modules.configureSecurity
import org.srino.modules.configureSerialization
import org.srino.upload.PostUploadManager

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    application = this

    database = Database()

    ktorUpload = KtorUpload {
        endpoint = dotenv["S3_ENDPOINT"]
        region = dotenv["S3_REGION"]
        accessKey = dotenv["S3_ACCESS_KEY"]
        secretKey = dotenv["S3_SECRET_KEY"]
        websiteEndpoint = dotenv["S3_WEBSITE_ENDPOINT"]
    }

    GsonManager.init()

    userManager = UserManager()
    sessionManager = SessionManager()
    postManager = PostManager()

    postUploadManager = PostUploadManager()


    configureSecurity()
    configureSerialization()
    configureRouting()

    onStop {
        Debug.send("Shutting down Application")

        userManager.shutdown()
        sessionManager.shutdown()
        postManager.shutdown()
    }
}

lateinit var application: Application
lateinit var database: Database

lateinit var userManager: UserManager
lateinit var postManager: PostManager
lateinit var sessionManager: SessionManager

lateinit var ktorUpload: KtorUpload

lateinit var postUploadManager: PostUploadManager

val dotenv = dotenv()