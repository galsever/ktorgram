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
import org.srino.upload.UserUploadManager
import java.io.File

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
    userUploadManager = UserUploadManager()

    share = Share()

    configureSecurity()
    configureSerialization()
    configureRouting()

    val root = File(".").absoluteFile

    val shareFolder = File(root, "share")
    if (!shareFolder.exists()) shareFolder.mkdirs()

    val defaultRequestFile = File(shareFolder, "request.ts")
    println(defaultRequestFile.readText(Charsets.UTF_8))

    val frontendRoot = root.parentFile.parentFile
    println(frontendRoot)
    val apiFolder = File(frontendRoot, "src/lib/api")
    if (!apiFolder.exists()) apiFolder.mkdirs()

    val definitionsFile = File(apiFolder, "definitions.ts")
    if (!definitionsFile.exists()) definitionsFile.createNewFile()

    val routeFile = File(apiFolder, "routes.ts")
    if (!routeFile.exists()) routeFile.createNewFile()

    val requestFile = File(apiFolder, "request.ts")
    if (!requestFile.exists()) defaultRequestFile.copyTo(requestFile, true)

    val routes = share.routes()
    routeFile.writeText(routes)
    val definitions = share.definitions()
    definitionsFile.writeText(definitions)
    println(definitions)
    println(routes)

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
lateinit var userUploadManager: UserUploadManager

lateinit var share: Share

val dotenv = dotenv()