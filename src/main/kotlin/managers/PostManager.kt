package org.srino.managers

import org.srino.application
import org.srino.database.managers.DatabaseManager
import org.srino.logic.Post
import java.util.UUID

class PostManager: DatabaseManager<UUID, Post>("posts", application, UUID::class.java, Post::class.java)