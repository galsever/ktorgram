package org.srino.upload

import org.srino.ktorUpload
import org.srino.ktorupload.manager.UploadManager
import org.srino.ktorupload.manager.mb
import kotlin.time.Duration.Companion.seconds

class PostUploadManager: UploadManager(ktorUpload, "temp", "posts", listOf(
    "image/jpeg",
    "image/png"
), 30.seconds, 20.mb)