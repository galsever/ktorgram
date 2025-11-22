package org.srino.logic

import org.srino.database.managers.DatabaseManager
import org.srino.database.managers.StringKey
import org.srino.database.managers.UUIDKey
import org.srino.ktorupload.logic.BucketObject
import org.srino.postManager
import java.util.*

class Post(
    val id: String,
    val owner: String,
    val content: String,
    val bucketObject: BucketObject
): StringKey<Post> {
    override fun id(): String = id
    override fun manager(): DatabaseManager<String, Post> = postManager
}