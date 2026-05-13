package com.example.proyectointegrador.models

data class WalkRequest(
    val id: String = "",
    val ownerId: String = "",
    val ownedName: String = "",
    val walkerId: String = "",
    val dogId: String = "",
    val dogName: String = "",
    val dogBreed: String = "",
    val dogSize: String = "",
    val dogAge: Int = 0,
    val dogAllergy: String = "",
    val durationMinutes: Int = 0,
    val cost: Double = 0.0,
    val status: String = "",     // "pendiente","aceptado","activo","finalizado"
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val paymentMethod: String = ""
)