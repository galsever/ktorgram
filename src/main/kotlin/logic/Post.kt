package org.srino.logic

import org.srino.database.managers.DatabaseManager
import org.srino.database.managers.UUIDKey
import org.srino.ktorupload.logic.BucketObject
import org.srino.postManager
import java.util.*

class Post(
    val id: UUID,
    val owner: String,
    val content: String,
    val bucketObject: BucketObject
): UUIDKey<Post> {
    override fun id(): UUID = id
    override fun manager(): DatabaseManager<UUID, Post> = postManager
}