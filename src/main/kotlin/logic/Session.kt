package org.srino.logic

data class Session(
    val id: String,
    val userId: String,
    val expiresAt: Long = System.currentTimeMillis() + 60 * 60 * 24 * 1000
)