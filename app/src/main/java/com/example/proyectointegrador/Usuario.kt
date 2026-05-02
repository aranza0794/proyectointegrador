package com.example.proyectointegrador


data class Usuario(
    val nombre: String = "",
    val correo: String = "",
    val fechaNacimiento: String = "",
    val telefono: String = "",
    val rol: String = "", // Aquí guardaremos "Paseador" o "Dueño"
    val tarjeta: String = "" // Este solo se llenará si es paseador
)