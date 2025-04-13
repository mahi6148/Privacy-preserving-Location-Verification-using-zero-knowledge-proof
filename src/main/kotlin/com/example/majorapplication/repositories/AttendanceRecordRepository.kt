package com.example.majorapplication.repositories

import com.example.majorapplication.models.AttendanceRecord
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AttendanceRecordRepository: CrudRepository<AttendanceRecord, Int>{
    fun findByUserUserIdAndSessionSessionId(userId: String, sessionId: Int): List<AttendanceRecord>

    /**
     * Count total attendance records for a specific session
     * @param sessionId The session ID to count records for
     * @return Total number of attendance records for the session
     */
    @Query("SELECT COUNT(*) FROM AttendanceRecord WHERE session.sessionId = :sessionId")
    fun countTotalAttendanceRecords(@Param("sessionId") sessionId: Int): Int

    /**
     * Count attendance records with 'PRESENT' status for a specific session
     * @param sessionId The session ID to count present records for
     * @return Number of 'PRESENT' attendance records for the session
     */
    @Query("SELECT COUNT(*) FROM AttendanceRecord WHERE session.sessionId = :sessionId AND status = 'PRESENT'")
    fun countPresentAttendanceRecords(@Param("sessionId") sessionId: Int): Int

}