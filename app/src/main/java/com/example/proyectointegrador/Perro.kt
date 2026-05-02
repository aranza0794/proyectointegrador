package com.example.proyectointegrador

data class Perro(
    val nombrePerro: String = "",
    val raza: String = "",
    val tamanio: String = "",
    val edad: Int = 0,
    val alergias: String = "",
    val idDueño: String = "" // Para saber a qué usuario le pertenece el perro
)