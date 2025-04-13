package com.example.majorapplication.grpcclient

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat.getString
import com.example.majorapplication.R
import com.example.majorapplication.messages.FetchSessionsRequestBody
import com.example.majorapplication.services.FetchSessionGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.time.LocalDateTime
import java.time.ZoneOffset

class GRPCFetchSession(context: Context) {

        private var channel: ManagedChannel?

        init{
            channel= ManagedChannelBuilder
                .forAddress(context.getString(R.string.serverip),9090)
                .usePlaintext()
                .build()
        }

        fun fetchSessions(userId:String):List<Session>{
            val returnedVal = FetchSessionGrpc.newBlockingStub(channel).fetchUserSessions(
                FetchSessionsRequestBody.newBuilder().setUserId(userId).build()
            )
            val sessionsList = returnedVal.sessionsList.toList().map { session ->
                Session(
                    sessionId = session.sessionId,
                    sessionName =session.sessionName,
                    startTime = LocalDateTime.ofEpochSecond(session.startedAt.seconds,session.startedAt.nanos, ZoneOffset.UTC),
                    endTime = LocalDateTime.ofEpochSecond(session.endedAt.seconds,session.endedAt.nanos, ZoneOffset.UTC),
                    locationLimit = session.locationLimit,
                    attendancePercentage = session.attendancePercentage
                )
            }
            return sessionsList
        }

    }

data class Session(
    val sessionId:Int,
    val sessionName:String,
    val startTime:LocalDateTime,
    val endTime: LocalDateTime,
    val locationLimit:Int,
    val attendancePercentage:Int
)