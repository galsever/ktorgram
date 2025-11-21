package org.srino.managers

import org.srino.application
import org.srino.database.managers.DatabaseManager
import org.srino.logic.User


class UserManager: DatabaseManager<String, User>("users", application, String::class.java, User::class.java) {

    val usersByUsername = mutableMapOf<String, User>()

    override fun onUpdate(key: String, value: User) {
        usersByUsername[value.username] = value
    }

    override fun onDataLoad() {
        this().values.forEach { usersByUsername[it.username] = it }
    }
}