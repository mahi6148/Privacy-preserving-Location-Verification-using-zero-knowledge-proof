package com.example.majorapplication.auth.models

data class SignInState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
)
