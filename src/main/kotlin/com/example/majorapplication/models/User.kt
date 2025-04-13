package com.example.majorapplication.models

import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.NoArgsConstructor
import java.time.LocalDateTime
import java.util.UUID

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name ="users")
@EqualsAndHashCode(exclude = ["sessions"])
data class User(
    @Id
    @Column(name = "user_id")
    val userId: String = "",

    @Column(name = "username", nullable = false, unique = true)
    val username: String="default user",

    @Column(name = "email", nullable = false, unique = true)
    val email: String = "default mail",

    @Column(name = "registered_on")
    val registeredOn: LocalDateTime = LocalDateTime.now(),

    @ManyToMany(fetch = FetchType.LAZY, cascade = [(CascadeType.ALL)])
    @JoinTable(
        name = "usersessions",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "session_id")]
    )
    var sessions: MutableSet<Session> = mutableSetOf()

){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as User
        return userId == other.userId
    }

    override fun hashCode(): Int {
        return userId.hashCode()
    }

    // Prevent data class from using collections in toString()
    override fun toString(): String {
        return "User(userId=$userId, username=$username, email=$email)"
    }

}