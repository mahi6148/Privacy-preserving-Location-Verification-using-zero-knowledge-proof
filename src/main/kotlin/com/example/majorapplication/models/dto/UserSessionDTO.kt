package com.example.majorapplication.models.dto

import com.example.majorapplication.models.Session
import com.example.majorapplication.models.User
import java.time.LocalDateTime

data class SessionDTO(
    val sessionId: Int?,
    val sessionName: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val locationLimit: Double?,
    val users: Set<UserDTO>
)

data class UserDTO(
    val userId: String,
    val username: String,
    val email: String
)

fun Session.toDTO(): SessionDTO {
    return SessionDTO(
        sessionId,
        sessionName,
        startTime,
        endTime,
        locationLimit,
        users.map { it.toDTO() }.toSet()
    )
}

fun User.toDTO(): UserDTO {
    return UserDTO(userId, username, email)
}