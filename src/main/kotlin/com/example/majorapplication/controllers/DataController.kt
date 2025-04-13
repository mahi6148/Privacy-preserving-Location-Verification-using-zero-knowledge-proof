package com.example.majorapplication.controllers

import com.example.majorapplication.models.AttendanceRecord
import com.example.majorapplication.models.AttendanceStatus
import com.example.majorapplication.models.Session
import com.example.majorapplication.zkp.BulletproofsBinding
import com.example.majorapplication.models.User
import com.example.majorapplication.repositories.AttendanceRecordRepository
import com.example.majorapplication.repositories.SessionRepository
import com.example.majorapplication.repositories.UserRepository
import com.example.majorapplication.services.SessionServiceHelper
import com.example.majorapplication.services.UserServiceHelper
import com.example.majorapplication.zkp.ProofData
import com.example.majorapplication.zkp.Verifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

@RestController
@RequestMapping("/data")
class DataController(private val sessionRepo:SessionRepository,private val userRepo: UserRepository,private val attendancerepo:AttendanceRecordRepository) {



    @GetMapping("/data")
    fun checkRustDll(){
//        val printable_proof =  bulletproofs.generateProofForCoordinates(12.823087, 80.044927,12.824595, 80.045152) // in range
        val printable_proof =  BulletproofsBinding.generateProofForCoordinates(12.823087, 80.044927,13.824595, 85.045152) // out of range
        println(printable_proof?.proofBytes)
    }

    @GetMapping("/insertUser")
   fun insertUser() {
       val userid = ""
        val name = (1..12)
            .map { Random.nextInt('!'.code, '~'.code + 1).toChar() }
            .joinToString("")
        val userTimeStamp = LocalDateTime.now()
       val user = User(userid,name,"sample@mail.id",userTimeStamp)
       userRepo.save(user)
    }

    @GetMapping("/deleteAllAttendance")
    suspend fun deleteAllAttendance() {
        coroutineScope {
            attendancerepo.deleteAll()
        }

    }

    @GetMapping("/deleteAllSessions")
    suspend fun deleteSessions(){
        coroutineScope {
            sessionRepo.deleteAll()
        }

    }

    @GetMapping("checkCoroutine")
    suspend fun checkCoroutine(){

        val coroutine = withContext(Dispatchers.IO){
            val user = userRepo.findById("")
            if (user.isEmpty){
                "no user"
            }else{
                user.toString()
            }
        }
        println(coroutine)
    }

    @GetMapping("/{userId}/sessions")
    fun getUserSessions(@PathVariable userId: String): ResponseEntity<Set<Session>> {
        val user = UserServiceHelper(userRepo).getUserWithSessions(userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user.sessions)
    }

    @GetMapping("/{sessionId}/users")
    fun getSessionUsers(@PathVariable sessionId: Int): ResponseEntity<Set<User>> {
        val session = SessionServiceHelper(sessionRepo).getSessionWithUsers(sessionId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(session.users)
    }

    @GetMapping("/insertAttendanceRecord")
    suspend fun insertAttendanceRecord(): ResponseEntity<AttendanceRecord> {
        val returnRecord = withContext(Dispatchers.IO){
            val user = userRepo.findById("96LYcwqra4QqLme0CtHE6LRaX0P2")
            val session = sessionRepo.findById(60)
            attendancerepo.save(AttendanceRecord(
                timestamp = LocalDateTime.now(),
                status = AttendanceStatus.ERROR,
                session = session.get(),
                user = user.get(),
            ))
        }
        return ResponseEntity.ok(returnRecord)
    }


}