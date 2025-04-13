package com.example.majorapplication.services

import com.example.majorapplication.messages.UserMessageRequestBody
import com.example.majorapplication.messages.UserMessageResponseBody
import com.example.majorapplication.models.User
import com.example.majorapplication.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.devh.boot.grpc.server.service.GrpcService
import java.time.LocalDateTime

@GrpcService
class UserService(private val userRepo: UserRepository):UserGrpcKt.UserCoroutineImplBase() {
    override suspend fun registerUser(request: UserMessageRequestBody): UserMessageResponseBody {

        val welcomeText = withContext(Dispatchers.IO){

           val user = userRepo.findById(request.userId)
            if (user.isEmpty){
                userRepo.save(User(
                    userId =request.userId,
                    username = request.userName,
                    email = request.email,
                    registeredOn = LocalDateTime.now(),
                ))
                "welcome ${request.userName}"

            }else{
                "welcome back ${request.userName}"
            }

        }

        return UserMessageResponseBody.newBuilder()
            .setMessage(welcomeText)
            .build()
    }
}