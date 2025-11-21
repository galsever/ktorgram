package org.srino.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.srino.ktorUpload
import org.srino.ktorupload.logic.Upload
import org.srino.ktorupload.logic.requests.PresignedURLRequest
import org.srino.ktorupload.logic.results.CopyResult
import org.srino.ktorupload.logic.results.PresignedUrlResult
import org.srino.logic.Post
import org.srino.modules.Routes
import org.srino.modules.UserSession
import org.srino.postManager
import org.srino.postUploadManager
import org.srino.user
import java.util.UUID

class PostRoutes: Routes {

    override fun Routing.routes() { authenticate("session-auth") {



        // CREATE
        // GET

        post("/posts") {
            user() ?: return@post
            val request = call.receive<PostCreateRequest>()

            val id = UUID.randomUUID()

            val response = postUploadManager.presignedUrl(id.toString(), request.uploadRequest.size, request.uploadRequest.contentType) { fileContentType, fileSize ->
                PostUpload(request.content, fileSize, fileContentType)
            }
            if (response.result != PresignedUrlResult.SUCCESS) return@post call.respond(HttpStatusCode.Unauthorized, "An error occurred while generating presigned url: ${response.result}")
            call.respond(PostCreateResponse(id, response.url!!))
        }

        post("/posts/finish") {
            val user = user() ?: return@post
            val request = call.receive<PostFinishRequest>()
            val response = postUploadManager.finishedUpload(request.id.toString())
            if (response.result != CopyResult.SUCCESS) return@post call.respond(HttpStatusCode.Unauthorized, "An error occurred while copying the file: ${response.result}")
            val postUpload = response.upload as PostUpload
            val post = Post(request.id, user.id, postUpload.content, response.bucketObject!!)
            post.register()

            call.respond(post)
        }

        get("/posts/{id}") {
            val postId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            val post = postId?.let { postManager[it] } ?: return@get call.respond(HttpStatusCode.NotFound, "post not found")

            call.respond(post)
        }
    } }
}

class PostUpload(contentType: String, size: Long, val content: String): Upload(contentType, size)

data class PostCreateRequest(
    val content: String,
    val uploadRequest: PresignedURLRequest
)

data class PostCreateResponse(val id: UUID, val url: String)

data class PostFinishRequest(val id: UUID)