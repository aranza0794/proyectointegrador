package com.example.proyectointegrador.models

data class Dog(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val breed: String = "",
    val size: String = "",       // "Pequeño", "Mediano", "Grande"
    val age: Int = 0,
    val allergy: String = ""
)