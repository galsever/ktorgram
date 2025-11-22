package org.srino.managers

import org.srino.application
import org.srino.database.managers.DatabaseManager
import org.srino.logic.Post

class PostManager: DatabaseManager<String, Post>("posts", application, String::class.java, Post::class.java)