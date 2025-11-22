package org.srino.upload

import org.srino.ktorUpload
import org.srino.ktorupload.manager.UploadManager
import org.srino.ktorupload.manager.mb
import kotlin.time.Duration.Companion.seconds

class UserUploadManager: UploadManager(ktorUpload, "temp", "profile_pictures", listOf(
    "image/jpeg",
    "image/png"
), 10.seconds, 3.mb) {
}