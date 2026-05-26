package com.example.proyectointegrador.models

data class WalkRequest(
    val id:              String = "",
    val ownerId:         String = "",
    val ownedName:       String = "",
    val dogId:           String = "",
    val dogName:         String = "",
    val dogBreed:        String = "",
    val dogSize:         String = "",
    val dogAge:          Int    = 0,
    val dogAllergy:      String = "",
    val durationMinutes: Int    = 0,
    val cost:            Double = 0.0,
    val paymentMethod:   String = ""
)