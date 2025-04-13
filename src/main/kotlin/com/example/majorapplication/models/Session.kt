package com.example.majorapplication.models

import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.NoArgsConstructor
import java.time.LocalDateTime

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = ["users"])
data class Session(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    val sessionId: Int? = null,

    @Column(name = "session_name", nullable = false)
    val sessionName: String = "",

    @Column(name = "start_time", nullable = false)
    val startTime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "end_time")
    var endTime: LocalDateTime? = null,

    @Column(name = "location_limit")
    val locationLimit: Double? = null,

    @ManyToMany(mappedBy = "sessions", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var users: MutableSet<User> = mutableSetOf()

){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Session
        return sessionId == other.sessionId
    }

    override fun hashCode(): Int {
        return sessionId.hashCode()
    }

    // Add this to prevent lazy loading during toString()
    override fun toString(): String {
        return "Session(sessionId=$sessionId, sessionName='$sessionName', startTime=$startTime, endTime=$endTime)"
    }

}
