package com.example.majorapplication.services

import com.example.majorapplication.messages.FetchSessionsRequestBody
import com.example.majorapplication.messages.FetchSessionsResponseBody
import com.example.majorapplication.messages.Session
import com.example.majorapplication.repositories.AttendanceRecordRepository
import com.example.majorapplication.repositories.SessionRepository
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import java.time.ZoneOffset

@GrpcService
class FetchSessionAttendanceService(private val sessionRepo:SessionRepository,private val attendanceRepo:AttendanceRecordRepository):FetchSessionGrpc.FetchSessionImplBase() {
    override fun fetchUserSessions(
        request: FetchSessionsRequestBody?,
        responseObserver: StreamObserver<FetchSessionsResponseBody>?
    ) {
        val sessionList = sessionRepo.findCompletedSessionsByUserId(request?.userId!!)
        val protoList = sessionList.map { session ->
            // Get total number of users registered for this session
            val totalRecords = attendanceRepo.countTotalAttendanceRecords(session.sessionId!!)
            // Get number of users who were present in this session
            val presentRecords = attendanceRepo.countPresentAttendanceRecords(session.sessionId)
            // Calculate attendance percentage (handle division by zero)
            val attendanceVal = if (totalRecords > 0) {
                (presentRecords.toDouble() / totalRecords.toDouble() * 100).toInt()
            } else {
                0
            }

            Session.newBuilder()
                .setSessionId(session.sessionId)
                .setSessionName(session.sessionName)
                .setStartedAt(Timestamp.newBuilder().setSeconds(session.startTime.toEpochSecond(ZoneOffset.UTC)).setNanos(session.startTime.nano))
                .apply {
                    session.endTime?.let {
                        setEndedAt(Timestamp.newBuilder().setSeconds(it.toEpochSecond(ZoneOffset.UTC)).setNanos(it.nano))
                    }
                }
                .setLocationLimit(session.locationLimit?.toInt() ?: 0)
                .setAttendancePercentage(attendanceVal)  // Add the attendance percentage to the protobuf
                .build()
        }

        val response = FetchSessionsResponseBody.newBuilder()
            .addAllSessions(protoList)
            .build()

        // Send the response and complete the RPC call
        responseObserver?.onNext(response)
        responseObserver?.onCompleted()

    }
}