package org.srino.managers

import org.srino.application
import org.srino.database.managers.DatabaseManager
import org.srino.logic.Session

class SessionManager: DatabaseManager<String, Session>("sessions", application, String::class.java, Session::class.java)