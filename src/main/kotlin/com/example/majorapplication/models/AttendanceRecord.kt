package com.example.majorapplication.models

import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import java.time.LocalDateTime


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendancerecords")
data class AttendanceRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    val recordId: Int? = null,

//    @Column(name = "user_id", nullable = false)
//    val userId: String = "", // Changed from User object to userId
//
//    @Column(name = "session_id", nullable = false)
//    val sessionId: Int? = Int.MIN_VALUE,

    @Column(name = "timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: AttendanceStatus = AttendanceStatus.DEFAULT,

    @ManyToOne
    @JoinColumn(name = "session_id")
    val session: Session?,

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

)

enum class AttendanceStatus {
    PRESENT, ABSENT, ERROR,DEFAULT
}
