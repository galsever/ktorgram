package org.srino.managers

import org.srino.application
import org.srino.database.managers.DatabaseManager
import org.srino.logic.User

class UserManager: DatabaseManager<String, User>("users", application, String::class.java, User::class.java)