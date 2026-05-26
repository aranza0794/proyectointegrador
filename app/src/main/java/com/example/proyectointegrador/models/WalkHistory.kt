package com.example.proyectointegrador.models

data class WalkHistory(
    val id:              String = "",
    val ownerId:         String = "",
    val walkerId:        String = "",
    val dogName:         String = "",
    val walkerName:      String = "",
    val ownedName:       String = "",
    val durationMinutes: Int    = 0,
    val cost:            Double = 0.0,
    val paymentMethod:   String = "",
    val status:          String = "",
    val ratingStars:     Int    = 0,
    val endTime:         Long   = 0L
)