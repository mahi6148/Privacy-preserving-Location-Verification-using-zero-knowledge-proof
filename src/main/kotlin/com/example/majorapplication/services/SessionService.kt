package com.example.majorapplication.services

import com.example.majorapplication.messages.SessionMessageRequestBody
import com.example.majorapplication.messages.SessionMessageResponseBody
import com.example.majorapplication.messages.StreamSessionRequest
import com.example.majorapplication.messages.StreamSessionResponse
import com.example.majorapplication.models.AttendanceRecord
import com.example.majorapplication.models.AttendanceStatus
import com.example.majorapplication.models.Session
import com.example.majorapplication.repositories.AttendanceRecordRepository
import com.example.majorapplication.repositories.SessionRepository
import com.example.majorapplication.repositories.UserRepository
import com.example.majorapplication.zkp.ProofData
import com.example.majorapplication.zkp.VerificationResult
import com.example.majorapplication.zkp.VerificationResult.Companion.toProto
import com.example.majorapplication.zkp.Verifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@GrpcService
class SessionService(private val sessionRepo: SessionRepository,private val attendanceRepo:AttendanceRecordRepository, private val userRepo:UserRepository): SessionGrpcKt.SessionCoroutineImplBase() {

    @Transactional(readOnly = false)
    override suspend fun requestSession(request: SessionMessageRequestBody): SessionMessageResponseBody = withContext(Dispatchers.IO) {
        println("session requested by client")

        val sessionName = request.sessionName
        val userId = request.userId
        val protoRequestedAt = request.sessionRequestedAt
        val ktRequestedAt = LocalDateTime.ofEpochSecond(protoRequestedAt.seconds, protoRequestedAt.nanos, ZoneOffset.UTC)

        // Create and save session
        val session = Session(
            sessionName = sessionName,
            startTime = ktRequestedAt
        )

        val savedSession =  try {
            sessionRepo.save(session)
        }catch (e: DataIntegrityViolationException){
            Session()
        }
        println("completed saving session")

        // Get the user and establish relationship in the same transaction
        if (savedSession.sessionId!=null){
            try {
                val user = userRepo.findById(userId).orElse(null)
                if (user != null) {
                    // Fix the User class to avoid the hashCode problem
                    sessionRepo.addUserToSession(sessionId = savedSession.sessionId, userId = userId)
                }

                // Optional: Update user side of relationship if needed
                // userRepo.save(user)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }

        println("Session-user relationship established")
        println(savedSession.sessionId.toString())

        SessionMessageResponseBody.newBuilder()
            .setSessionId(savedSession.sessionId?:-1)
            .setLatitude(12.824571)
            .setLongitude(80.045217)
            .setSessionName(savedSession.sessionName)
            .build()
    }
//
//
//    {
//        println("session requested by client")
//
//        val sessionName = request.sessionName
//        val userId = request.userId
//        val protoRequestedAt = request.sessionRequestedAt
//        val ktRequestedAt = LocalDateTime.ofEpochSecond(protoRequestedAt.seconds,protoRequestedAt.nanos,ZoneOffset.UTC)
//
//       val returnedSession = withContext(Dispatchers.IO) {
//            sessionRepo.save(
//                Session(
//                    sessionName = sessionName,
//                    startTime = ktRequestedAt
//                )
//            )
//        }
//        println("completed saving session")
//         try {
//             withContext(Dispatchers.IO){
//                 val user = userRepo.findById(userId).orElse(null)
//                 returnedSession.users.add(user)
//                 user?.sessions?.add(returnedSession)
//
//                 // Save session (will cascade to user if configured properly)
//                 sessionRepo.save(returnedSession)
//
//             }
//         }catch (e:Exception){
//             e.printStackTrace()
//             println(e.message)
//         }
//
//        println("mistake happened above")
//
//
//
//        return SessionMessageResponseBody.newBuilder()
//            .setSessionId(returnedSession.sessionId.toString())
//            .setLatitude(12.824571)
//            .setLongitude(80.045217)
//            .setSessionName(sessionName)
//            .build()
//    }


    override fun streamToSession(requests: Flow<StreamSessionRequest>): Flow<StreamSessionResponse> = channelFlow<StreamSessionResponse> {

        coroutineScope {
            val sharedRequests = requests.shareIn(this, SharingStarted.Eagerly)

            // Capture session ID from the first request
            var sessionId: Int? = sharedRequests
                .take(1)
                .firstOrNull()
                ?.sessionId
                ?.toInt()

            println("Captured Session ID: $sessionId")
            try {
                sharedRequests.collect { request ->
                    // Capture session ID for potential later use
                    sessionId = request.sessionId.toInt()

                    // Existing processing logic...
                    ensureActive()

                    val userId = request.userId
                    val requestedAt = LocalDateTime.ofEpochSecond(
                        request.recordedTimestamp.seconds,
                        request.recordedTimestamp.nanos,
                        ZoneOffset.UTC
                    )

                    val proof = ProofData(
                        request.data.proofBytes.toByteArray(),
                        request.data.ristrettoBytes.toByteArray()
                    )

                    val verificationResult = Verifier.verify(proof)
                    val attendanceStatus = when (verificationResult) {
                        VerificationResult.Present -> AttendanceStatus.PRESENT
                        VerificationResult.Absent -> AttendanceStatus.ABSENT
                        is VerificationResult.Error -> AttendanceStatus.ERROR
                    }

                    withContext(Dispatchers.IO) {
                        val user = userRepo.findById(userId).orElse(null)
                        println("user is: $user")
                        val session = sessionRepo.findById(sessionId!!).get()
                        println("session is: $session")
                        attendanceRepo.save(
                            AttendanceRecord(
                                timestamp = requestedAt,
                                status = attendanceStatus,
                                user = user,
                                session = session
                            )
                        )
                    }

                    val emittingResponse = verificationResult.toProto()
                    emittingResponse.let {
                        send(it)
                    }
                }
            } catch (e: CancellationException) {
                println("Stream cancelled: ${e.message}")
                println("session Id is : $sessionId")
                // Update session end time
                sessionId?.let { id ->
                    try {
                        coroutineScope {
                            println("entered session update try block")
                            sessionRepo.updateSessionEndTime(id, LocalDateTime.now())
                            println("session update line executed")
                            println("Session successfully updated: $id")
                        }
                    } catch (updateEx: Exception) {
                        if (updateEx !is CancellationException) {
                            println("Error updating session: ${updateEx.message}")
                            updateEx.printStackTrace()
                        }
                    }
                }

                // Rethrow to propagate cancellation
                throw e
            } catch (e: Exception) {
                println("Stream processing error: ${e.message}")
                e.printStackTrace()

                // Update session end time
                sessionId?.let { id ->
                    try {
                        val updatedSession = withContext(Dispatchers.IO) {
                            val session = sessionRepo.findById(id)
                            if (session.isPresent) {
                                val existingSession = session.get()
                                existingSession.endTime = LocalDateTime.now()
                                sessionRepo.save(existingSession)
                            }
                            session.orElse(null)
                        }
                        println("Session updated: ${updatedSession?.sessionId}")
                    } catch (updateEx: Exception) {
                        println("Error updating session: ${updateEx.message}")
                    }
                }

                throw e
            }
        }

//        launch {
//            try {
//                println("stream hit")
//                requests.collect { request ->
//                    println("request collected")
//                    val sessionId = request.sessionId
//                    val userId = request.userId
//                    val requestedAt = LocalDateTime.ofEpochSecond(
//                        request.recordedTimestamp.seconds,
//                        request.recordedTimestamp.nanos,
//                        ZoneOffset.UTC
//                    )
//                    println("timestamp created")
//                    val proof = ProofData(request.data.proofBytes.toByteArray(), request.data.ristrettoBytes.toByteArray())
//
//                    val verificationResult = Verifier.verify(proof)
//                    println("verificationResult obtained")
//                    val attendanceStatus = when (verificationResult) {
//                        VerificationResult.Present -> AttendanceStatus.PRESENT
//                        VerificationResult.Absent -> AttendanceStatus.ABSENT
//                        is VerificationResult.Error -> AttendanceStatus.ERROR
//                    }
//
//                    withContext(Dispatchers.IO){
//                        println("will start attendance")
//                        attendanceRepo.save(
//                            AttendanceRecord(
//                                userId = userId,
//                                sessionId = sessionId.toInt(),
//                                timestamp = requestedAt,
//                                status = attendanceStatus
//                            )
//                        )
//                        println("attendance saved successfully")
//                    }
//
//                    val emittingResponse = verificationResult.toProto()
//                    send(emittingResponse)
//                    println("done")
//                }
//            }catch (t:Throwable){
//                println(t.message)
//                requests.collect{request ->
//                    val sessionId = request.sessionId
//                    val record = withContext(Dispatchers.IO){
//                        sessionRepo.findById(sessionId.toInt())
//                    }
//                    record.get().endTime = LocalDateTime.now()
//                    withContext(Dispatchers.IO){
//                        if (!record.isEmpty){
//                            sessionRepo.save(
//                                record.get()
//                            )
//                        }
//                    }
//
//                }
//            }finally {
//                requests.collect{request ->
//                    val sessionId = request.sessionId
//                    val record = withContext(Dispatchers.IO){
//                        sessionRepo.findById(sessionId.toInt())
//                    }
//                    record.get().endTime = LocalDateTime.now()
//                    withContext(Dispatchers.IO){
//                        if (!record.isEmpty){
//                            sessionRepo.save(
//                                record.get()
//                            )
//                        }
//                    }
//
//                }
//            }
//        }
    }.cancellable()

}