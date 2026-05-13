package com.example.proyectointegrador.models

data class Rating(
    val id: String = "",
    val walkRequestId: String = "",
    val walkerId: String = "",
    val ownerName: String = "",
    val stars: Int = 0,
    val comment: String = ""
)