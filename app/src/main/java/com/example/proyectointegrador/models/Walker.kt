package com.example.proyectointegrador.models

data class Walker(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val cardNumber: String = "",
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val isAvailable: Boolean = true
)