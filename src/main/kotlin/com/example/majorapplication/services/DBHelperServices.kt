package com.example.majorapplication.services

import com.example.majorapplication.models.AttendanceRecord
import com.example.majorapplication.models.AttendanceStatus
import com.example.majorapplication.models.Session
import com.example.majorapplication.models.User
import com.example.majorapplication.repositories.AttendanceRecordRepository
import com.example.majorapplication.repositories.SessionRepository
import com.example.majorapplication.repositories.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceHelper(private val userRepository: UserRepository) {

    @Transactional(readOnly = true)
    fun getUserWithSessions(userId: String): User? {
        return userRepository.findByIdWithSessions(userId)
    }

    @Transactional
    fun registerUserToSession(userId: String, session: Session) {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User with ID $userId not found")
        }
        user.sessions.add(session)
        userRepository.save(user)
    }
}

@Service
class SessionServiceHelper(private val sessionRepository: SessionRepository) {

    @Transactional(readOnly = true)
    fun getSessionWithUsers(sessionId: Int): Session? {
        return sessionRepository.findByIdWithUsers(sessionId)
    }

    @Transactional
    fun addUserToSession(user: User, sessionId: Int) {
        val session = sessionRepository.findById(sessionId).orElseThrow {
            IllegalArgumentException("Session with ID $sessionId not found")
        }
        session.users.add(user)
        sessionRepository.save(session)
    }
}

@Service
class AttendanceServiceHelper(
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    @Transactional
    fun recordAttendance(userId: String, sessionId: Int, status: AttendanceStatus): AttendanceRecord {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User with ID $userId not found")
        }

        val session = sessionRepository.findById(sessionId).orElseThrow {
            IllegalArgumentException("Session with ID $sessionId not found")
        }

        val record = AttendanceRecord(
            user = user,
            status = status,
            session = session
        )

        return attendanceRecordRepository.save(record)
    }

    @Transactional(readOnly = true)
    fun getAttendanceRecords(userId: String, sessionId: Int): List<AttendanceRecord> {
        return attendanceRecordRepository.findByUserUserIdAndSessionSessionId(userId, sessionId)
    }
}