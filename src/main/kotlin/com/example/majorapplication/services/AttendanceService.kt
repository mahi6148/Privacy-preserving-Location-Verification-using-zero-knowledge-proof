package com.example.majorapplication.services

import com.example.majorapplication.messages.AttendanceMessageRequestBody
import com.example.majorapplication.messages.AttendanceMessageResponseBody
import com.example.majorapplication.zkp.ProofData
import com.example.majorapplication.zkp.VerificationResult
import com.example.majorapplication.zkp.Verifier
import net.devh.boot.grpc.server.service.GrpcService


@GrpcService
class AttendanceService:AttendanceGrpcKt.AttendanceCoroutineImplBase(){
    override suspend fun checkAttendance(request: AttendanceMessageRequestBody): AttendanceMessageResponseBody {

        println("Attendance request got hit from client")
        val verificationResult = Verifier.verify(ProofData(
            request.data.proofBytes.toByteArray(),
            request.data.ristrettoBytes.toByteArray()
        ))



        val resultString:String = when(verificationResult){
            VerificationResult.Present -> "Present"
            VerificationResult.Absent -> "Absent"
            is VerificationResult.Error -> "Error"

        }





        println("processing done")

        return AttendanceMessageResponseBody.newBuilder()
            .setAttendanceStatus(resultString)
            .build()
    }
}
