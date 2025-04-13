package com.example.majorapplication.grpcclient


import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import com.example.majorapplication.R
import com.example.majorapplication.messages.UserMessageRequestBody
import com.example.majorapplication.services.UserGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

class GrpcUser (context: Context){
        private var channel:ManagedChannel?
        init {
            channel = ManagedChannelBuilder
                .forAddress(context.getString(R.string.serverip),9090)
                .usePlaintext()
                .build()
        }

        fun sendUserRequest(userId:String,userName:String,userEmail:String,){
            val returnedVal = UserGrpc.newBlockingStub(channel).registerUser(
                UserMessageRequestBody.newBuilder()
                    .setUserId(userId)
                    .setUserName(userName)
                    .setEmail(userEmail)
                    .build()
            )
            Log.d(TAG,returnedVal.message)
        }

        fun terminateChannel(){
            if (channel ==null) {return}
            channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
            channel = null
        }
}