package com.example.majorapplication.repositories

import com.example.majorapplication.models.Session

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface SessionRepository :CrudRepository<Session,Int> {
    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.endTime = :endTime WHERE s.sessionId = :sessionId")
    fun updateSessionEndTime(sessionId: Int, endTime: LocalDateTime)

    @Modifying
    @Transactional
    @Query("""
        INSERT INTO usersessions (session_id, user_id) 
        VALUES (:sessionId, :userId)
    """, nativeQuery = true)
    fun addUserToSession(@Param("sessionId") sessionId: Int, @Param("userId") userId: String)

    @Query("""
        SELECT s FROM Session s 
        JOIN s.users u 
        WHERE u.userId = :userId AND s.endTime IS NOT NULL
    """)
    fun findCompletedSessionsByUserId(@Param("userId") userId: String): List<Session>

    // Native SQL version
    @Query("""
        SELECT s.* FROM session s
        JOIN usersessions us ON s.session_id = us.session_id
        WHERE us.user_id = :userId AND s.end_time IS NOT NULL
    """, nativeQuery = true)
    fun findCompletedSessionsByUserIdNative(@Param("userId") userId: String): List<Session>


    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.users WHERE s.sessionId = :sessionId")
    fun findByIdWithUsers(@Param("sessionId") sessionId: Int): Session?

    @Query("""
        SELECT s FROM Session s
        JOIN s.users u
        WHERE u.userId = :userId
    """)
    fun findSessionsByUserId(@Param("userId") userId: String): List<Session>
//
    // Alternative using native SQL query
    @Query("""
        SELECT s.* FROM session s
        JOIN usersessions us ON s.session_id = us.session_id
        WHERE us.user_id = :userId
    """, nativeQuery = true)
    fun findSessionsByUserIdNative(@Param("userId") userId: String): List<Session>

}