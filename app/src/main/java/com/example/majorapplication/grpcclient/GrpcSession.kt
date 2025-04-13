package com.example.majorapplication.grpcclient


import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.majorapplication.R
import com.example.majorapplication.location.PermissionHelper
import com.example.majorapplication.messages.AttendanceMessageRequestBody
import com.example.majorapplication.messages.SessionMessageRequestBody
import com.example.majorapplication.messages.StreamSessionRequest
import com.example.majorapplication.messages.StreamSessionResponse
import com.example.majorapplication.messages.VerificationResultType
import com.example.majorapplication.services.AttendanceGrpc
import com.example.majorapplication.services.SessionGrpc
import com.example.majorapplication.zkp.ProofData
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.util.concurrent.TimeUnit

class GrpcSession (val context: Context){

       private var channel:ManagedChannel?

        init {
            channel= ManagedChannelBuilder
                 .forAddress(context.getString(R.string.serverip),9090)
                 .usePlaintext()
                 .build()
        }


        fun sendSessionReq(userId:String,sessionName:String):Pair<Pair<Double,Double>,Pair<Int,String>>?{
            if (!PermissionHelper.checkLocationServices(context = context)){
                Toast.makeText(context,"Enable Location and Internet services",Toast.LENGTH_LONG).show()
                return null}
            val now = Instant.now()
            val returnedGrpcVal = SessionGrpc.newBlockingStub(channel).requestSession(SessionMessageRequestBody.newBuilder()
                .setUserId(userId)
                .setSessionRequestedAt(Timestamp.newBuilder().setSeconds(now.epochSecond))
                .setSessionName(sessionName)
                .build())
            Log.d(TAG,returnedGrpcVal.toString())

            val lat = returnedGrpcVal.latitude
            val lon = returnedGrpcVal.longitude
            val id = returnedGrpcVal.sessionId
            val name = returnedGrpcVal.sessionName
            return Pair(Pair(lat,lon),Pair(id,name))
        }

        fun sendAttendanceRequest(data:ProofData,userId: String){
            val now = Instant.now()
            val returnedGrpcVal = AttendanceGrpc.newBlockingStub(channel).checkAttendance(
                AttendanceMessageRequestBody.newBuilder()
                    .setData(com.example.majorapplication.messages.ProofData.newBuilder().setProofBytes(ByteString.copyFrom(data.proofBytes)).setRistrettoBytes(ByteString.copyFrom(data.ristrettoBytes)))
                    .setUserId(userId)
                    .setRecordedTimestamp(Timestamp.newBuilder().setSeconds(now.epochSecond))
                    .build()
            )

            Log.d(TAG,"returned GRPCVal : $returnedGrpcVal")
        }


        fun streamSession(verificationResultFlow:MutableStateFlow<VerificationResult>,isStreaming: MutableStateFlow<Boolean>,updateNotification:(String)->Unit,sessionName:String): StreamObserver<StreamSessionRequest>? {
            isStreaming.value = true
            return SessionGrpc.newStub(channel).streamToSession(
                object : StreamObserver<StreamSessionResponse> {
                    override fun onNext(value: StreamSessionResponse?) {
                        verificationResultFlow.value = value?.toKotlin()!!
                        when(value.toKotlin()){
                            VerificationResult.Absent ->updateNotification("Session: $sessionName - Absent ❌")
                            VerificationResult.Present -> updateNotification("Session: $sessionName - Present ✓")
                            is VerificationResult.Error -> updateNotification("Session: $sessionName - Error: ${(value.toKotlin() as VerificationResult.Error).errorType.name}")

                        }
                    }

                    override fun onError(t: Throwable?) {
                        if (t?.message == "CANCELLED: Cancelled by client with StreamObserver.onError()"){
                            verificationResultFlow.value = VerificationResult.Error(ErrorType.START_STREAMING_DEFAULT)
                            return
                        }
                       Log.d("Stream Error",t?.message.toString())
                        verificationResultFlow.value = VerificationResult.Error(ErrorType.STREAM_ERROR)
                    }

                    override fun onCompleted() {
                        verificationResultFlow.value = VerificationResult.Error(ErrorType.STREAM_COMPLETED)
                        isStreaming.value = false
                    }

                }
            )

//            return returningResult
        }


      fun terminateChannel(){
            if (channel==null) {return}
            channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
            channel = null
        }

    }

fun VerificationResult.toProto(): StreamSessionResponse {
    return when (this) {
        VerificationResult.Present -> StreamSessionResponse.newBuilder()
            .setResult(VerificationResultType.PRESENT)
            .build()

        VerificationResult.Absent -> StreamSessionResponse.newBuilder()
            .setResult(VerificationResultType.ABSENT)
            .build()

        is VerificationResult.Error -> StreamSessionResponse.newBuilder()
            .setResult(VerificationResultType.ERROR)
            .setError(
                when(this.errorType){
                    ErrorType.PROOF_ERROR -> com.example.majorapplication.messages.ErrorType.PROOF_ERROR
                    ErrorType.DESERIALIZATION_ERROR -> com.example.majorapplication.messages.ErrorType.DESERIALIZATION_ERROR
                    ErrorType.EMPTY_VECTORS_ERROR -> com.example.majorapplication.messages.ErrorType.EMPTY_VECTORS_ERROR
                    ErrorType.JAVA_TO_RUST_CONVERSION_ERROR -> com.example.majorapplication.messages.ErrorType.JAVA_TO_RUST_CONVERSION_ERROR
                    else -> com.example.majorapplication.messages.ErrorType.UNRECOGNIZED
                }
            )
            .build()
    }
}

// Conversion from protobuf message back to Kotlin VerificationResult
fun StreamSessionResponse.toKotlin(): VerificationResult {
    return when (this.result) {
        VerificationResultType.PRESENT -> VerificationResult.Present
        VerificationResultType.ABSENT -> VerificationResult.Absent
        VerificationResultType.ERROR -> VerificationResult.Error(
            when (this.error) {
                com.example.majorapplication.messages.ErrorType.JAVA_TO_RUST_CONVERSION_ERROR -> ErrorType.JAVA_TO_RUST_CONVERSION_ERROR
                com.example.majorapplication.messages.ErrorType.EMPTY_VECTORS_ERROR -> ErrorType.EMPTY_VECTORS_ERROR
                com.example.majorapplication.messages.ErrorType.DESERIALIZATION_ERROR -> ErrorType.DESERIALIZATION_ERROR
                com.example.majorapplication.messages.ErrorType.PROOF_ERROR -> ErrorType.PROOF_ERROR
                else -> throw IllegalArgumentException("Unknown error type")
            }
        )
        else -> throw IllegalArgumentException("Unknown verification result type")
    }
}

