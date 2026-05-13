package com.example.proyectointegrador.models

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val birthDate: String = "",
    val userType: String = "",   // "Dueño" o "Paseador"
    val cardNumber: String = ""  // Solo paseadores
)