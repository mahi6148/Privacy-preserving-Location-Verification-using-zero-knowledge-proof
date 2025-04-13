package com.example.majorapplication.repositories

import com.example.majorapplication.models.User
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : CrudRepository<User, String>{
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.sessions WHERE u.userId = :userId")
    fun findByIdWithSessions(@Param("userId") userId: String): User?
}
