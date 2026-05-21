package com.example.proyectointegrador.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.adapters.Comment
import com.example.proyectointegrador.adapters.CommentsAdapter
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var photoBase64 = ""

    // FIX: Lanzador para elegir foto de la galería
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let { processAndSavePhoto(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        session = SessionManager(this)

        val toolbar        = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mi perfil"

        val ivProfilePhoto  = findViewById<ImageView>(R.id.ivProfilePhoto)
        val tvProfileInitial = findViewById<TextView>(R.id.tvProfileInitial)
        val tvProfileName   = findViewById<TextView>(R.id.tvProfileName)
        val tvProfileType   = findViewById<TextView>(R.id.tvProfileType)
        val tvProfileEmail  = findViewById<TextView>(R.id.tvProfileEmail)
        val tvProfilePhone  = findViewById<TextView>(R.id.tvProfilePhone)
        val layoutRating    = findViewById<View>(R.id.layoutWalkerRating)
        val tvRatingTitle   = findViewById<TextView>(R.id.tvRatingTitle)
        val tvCommentsLabel = findViewById<TextView>(R.id.tvCommentsLabel)
        val btnChangePhoto  = findViewById<MaterialCardView>(R.id.btnChangePhoto)
        val btnEditProfile  = findViewById<MaterialButton>(R.id.btnEditProfile)
        val btnLogout       = findViewById<MaterialButton>(R.id.btnLogout)

        // Cargar datos
        db.collection("usuarios").document(session.getUserId()).get()
            .addOnSuccessListener { doc ->
                val name      = doc.getString("name") ?: ""
                val email     = doc.getString("email") ?: ""
                val phone     = doc.getString("phone") ?: ""
                val userType  = doc.getString("userType") ?: ""
                val rating    = doc.getDouble("rating") ?: 0.0
                val ratingCount = doc.getLong("ratingCount") ?: 0L
                val photo     = doc.getString("photoBase64") ?: ""

                tvProfileName.text  = name
                tvProfileEmail.text = email
                tvProfilePhone.text = phone
                tvProfileType.text  = if (userType == "owner")
                    "🐶 Dueño de perro" else "🦺 Paseador"

                // FIX: Mostrar foto o inicial
                if (photo.isNotEmpty()) {
                    loadBase64Photo(photo, ivProfilePhoto, tvProfileInitial)
                } else {
                    tvProfileInitial.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                }

                // Calificaciones para ambos tipos
                layoutRating.visibility = View.VISIBLE
                tvRatingTitle.text = if (userType == "owner")
                    "Calificación como dueño" else "Calificación como paseador"
                tvCommentsLabel.text = if (userType == "owner")
                    "COMENTARIOS DE PASEADORES" else "COMENTARIOS DE DUEÑOS"

                loadRatings(rating, ratingCount)
            }

        // FIX: Botón para cambiar foto
        btnChangePhoto.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cambiar foto de perfil")
                .setItems(arrayOf("📷 Tomar foto", "🖼️ Elegir de galería")) { _, which ->
                    when (which) {
                        0 -> openCamera()
                        1 -> openGallery()
                    }
                }
                .show()
        }

        btnEditProfile.setOnClickListener { showEditDialog() }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres cerrar sesión?")
                .setPositiveButton("Sí, salir") { _, _ ->
                    session.clearSession()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    // FIX: Abrir galería
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    // FIX: Abrir cámara
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        pickImageLauncher.launch(intent)
    }

    // FIX: Comprimir foto y convertir a Base64
    private fun processAndSavePhoto(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Comprimir a máximo 300x300 para que no sea muy pesado
            val resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            // Guardar en Firestore
            db.collection("usuarios").document(session.getUserId())
                .update("photoBase64", base64)
                .addOnSuccessListener {
                    Toast.makeText(this, "✓ Foto actualizada", Toast.LENGTH_SHORT).show()
                    // Mostrar foto nueva
                    val ivProfilePhoto   = findViewById<ImageView>(R.id.ivProfilePhoto)
                    val tvProfileInitial = findViewById<TextView>(R.id.tvProfileInitial)
                    loadBase64Photo(base64, ivProfilePhoto, tvProfileInitial)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar foto", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    // FIX: Decodificar Base64 y mostrar en ImageView
    private fun loadBase64Photo(base64: String, ivPhoto: ImageView, tvInitial: TextView) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ivPhoto.setImageBitmap(bitmap)
            ivPhoto.visibility  = View.VISIBLE
            tvInitial.visibility = View.GONE
        } catch (e: Exception) {
            ivPhoto.visibility  = View.GONE
            tvInitial.visibility = View.VISIBLE
        }
    }

    private fun loadRatings(rating: Double, ratingCount: Long) {
        val tvRatingScore    = findViewById<TextView>(R.id.tvRatingScore)
        val ratingBarProfile = findViewById<RatingBar>(R.id.ratingBarProfile)
        val tvRatingCount    = findViewById<TextView>(R.id.tvRatingCount)
        val rvComments       = findViewById<RecyclerView>(R.id.rvComments)

        tvRatingScore.text      = String.format("%.1f", rating)
        ratingBarProfile.rating = rating.toFloat()
        tvRatingCount.text      = if (ratingCount == 0L)
            "Sin calificaciones aún"
        else "$ratingCount calificación${if (ratingCount > 1) "es" else ""}"

        rvComments.layoutManager            = LinearLayoutManager(this)
        rvComments.isNestedScrollingEnabled = false

        db.collection("calificaciones")
            .whereEqualTo("ratedUserId", session.getUserId())
            .get()
            .addOnSuccessListener { docs ->
                val comments = docs.documents
                    .map { doc ->
                        Comment(
                            ownerName = doc.getString("ownerName") ?: "Usuario",
                            stars     = (doc.getLong("stars") ?: 0L).toInt(),
                            comment   = doc.getString("comment") ?: ""
                        )
                    }
                    .filter { it.stars > 0 }
                    .sortedByDescending { it.stars }

                if (comments.isNotEmpty()) {
                    rvComments.adapter = CommentsAdapter(comments)
                }
            }
    }

    private fun showEditDialog() {
        val dialogView   = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etName       = dialogView.findViewById<TextInputEditText>(R.id.etName)
        val etPhone      = dialogView.findViewById<TextInputEditText>(R.id.etPhone)
        val etBirthDate  = dialogView.findViewById<TextInputEditText>(R.id.etBirthDate)
        val tilCard      = dialogView.findViewById<TextInputLayout>(R.id.tilCardNumber)
        val etCardNumber = dialogView.findViewById<TextInputEditText>(R.id.etCardNumber)

        if (session.getUserType() == "walker") tilCard.visibility = View.VISIBLE

        db.collection("usuarios").document(session.getUserId()).get()
            .addOnSuccessListener { doc ->
                etName.setText(doc.getString("name") ?: "")
                etPhone.setText(doc.getString("phone") ?: "")
                etBirthDate.setText(doc.getString("birthDate") ?: "")
                if (session.getUserType() == "walker") {
                    etCardNumber.setText(doc.getString("cardNumber") ?: "")
                }
            }

        AlertDialog.Builder(this)
            .setTitle("Editar mis datos")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name      = etName.text.toString().trim()
                val phone     = etPhone.text.toString().trim()
                val birthDate = etBirthDate.text.toString().trim()
                val card      = etCardNumber.text.toString().trim()

                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this,
                        "Nombre y teléfono son requeridos",
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updates = hashMapOf<String, Any>(
                    "name" to name, "phone" to phone, "birthDate" to birthDate
                )
                if (session.getUserType() == "walker" && card.isNotEmpty()) {
                    updates["cardNumber"] = card
                }

                db.collection("usuarios").document(session.getUserId())
                    .update(updates)
                    .addOnSuccessListener {
                        session.saveSession(
                            session.getUserId(), session.getUserType(), name)
                        Toast.makeText(this, "✓ Datos actualizados", Toast.LENGTH_SHORT).show()
                        recreate()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}