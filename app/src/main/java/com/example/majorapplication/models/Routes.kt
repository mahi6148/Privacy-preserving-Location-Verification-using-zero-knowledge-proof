package com.example.majorapplication.models

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object loginScreen: Route
    @Serializable
    data object profileScreen: Route
    @Serializable
    data object Auth: Route
    @Serializable
    data object Main: Route
    @Serializable
    data object Home:Route
    @Serializable
    data object Activity:Route
}