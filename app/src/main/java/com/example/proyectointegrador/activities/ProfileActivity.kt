package com.example.proyectointegrador.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.adapters.Comment
import com.example.proyectointegrador.adapters.CommentsAdapter
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) processPhoto(uri)
            else (result.data?.extras?.get("data") as? Bitmap)?.let { savePhoto(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        session = SessionManager(this)

        if (session.getUserId().isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        val toolbar          = findViewById<Toolbar>(R.id.toolbar)
        // FIX: todos los tipos correctos según el XML actual
        val ivProfilePhoto   = findViewById<ImageView>(R.id.ivProfilePhoto)
        val tvProfileInitial = findViewById<TextView>(R.id.tvProfileInitial)
        val tvProfileName    = findViewById<TextView>(R.id.tvProfileName)
        val tvProfileType    = findViewById<TextView>(R.id.tvProfileType)
        val tvProfileEmail   = findViewById<TextView>(R.id.tvProfileEmail)
        val tvProfilePhone   = findViewById<TextView>(R.id.tvProfilePhone)
        val layoutRating     = findViewById<View>(R.id.layoutWalkerRating)
        val tvRatingTitle    = findViewById<TextView>(R.id.tvRatingTitle)
        val tvCommentsLabel  = findViewById<TextView>(R.id.tvCommentsLabel)
        // FIX: MaterialButton no MaterialCardView
        val btnChangePhoto   = findViewById<MaterialButton>(R.id.btnChangePhoto)
        val btnEditProfile   = findViewById<MaterialButton>(R.id.btnEditProfile)
        val btnLogout        = findViewById<MaterialButton>(R.id.btnLogout)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mi perfil"

        loadProfile(
            ivProfilePhoto, tvProfileInitial, tvProfileName,
            tvProfileType, tvProfileEmail, tvProfilePhone,
            layoutRating, tvRatingTitle, tvCommentsLabel
        )

        btnChangePhoto.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Foto de perfil")
                .setItems(arrayOf("📷 Tomar foto", "🖼️ Elegir de galería")) { _, which ->
                    when (which) {
                        0 -> pickImageLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                        1 -> pickImageLauncher.launch(
                            Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                .also { it.type = "image/*" })
                    }
                }.show()
        }

        btnEditProfile.setOnClickListener { showEditDialog() }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres cerrar sesión?")
                .setPositiveButton("Sí, salir") { _, _ ->
                    session.clearSession()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun loadProfile(
        ivPhoto: ImageView, tvInitial: TextView, tvName: TextView,
        tvType: TextView, tvEmail: TextView, tvPhone: TextView,
        layoutRating: View, tvRatingTitle: TextView, tvCommentsLabel: TextView
    ) {
        db.collection("usuarios").document(session.getUserId()).get()
            .addOnSuccessListener { doc ->
                if (doc == null || !doc.exists()) {
                    Toast.makeText(this, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val name        = doc.getString("name")       ?: ""
                val email       = doc.getString("email")      ?: ""
                val phone       = doc.getString("phone")      ?: ""
                val userType    = doc.getString("userType")   ?: session.getUserType()
                val rating      = doc.getDouble("rating")     ?: 0.0
                val ratingCount = doc.getLong("ratingCount")  ?: 0L
                val photo       = doc.getString("photoBase64")?: ""

                tvName.text  = name
                tvEmail.text = email
                tvPhone.text = if (phone.isNotEmpty()) phone else "No registrado"
                tvType.text  = if (userType == "owner") "🐶 Dueño de perro" else "🦺 Paseador"

                if (photo.isNotEmpty()) {
                    loadBase64(photo, ivPhoto, tvInitial)
                } else {
                    tvInitial.text      = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    ivPhoto.visibility  = View.GONE
                    tvInitial.visibility= View.VISIBLE
                }

                layoutRating.visibility  = View.VISIBLE
                tvRatingTitle.text = if (userType == "owner")
                    "Calificación como dueño" else "Calificación como paseador"
                tvCommentsLabel.text = if (userType == "owner")
                    "COMENTARIOS DE PASEADORES" else "COMENTARIOS DE DUEÑOS"

                loadRatings(rating, ratingCount)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadRatings(rating: Double, ratingCount: Long) {
        val tvRatingScore    = findViewById<TextView>(R.id.tvRatingScore)
        val ratingBarProfile = findViewById<RatingBar>(R.id.ratingBarProfile)
        val tvRatingCount    = findViewById<TextView>(R.id.tvRatingCount)
        val rvComments       = findViewById<RecyclerView>(R.id.rvComments)

        tvRatingScore.text      = String.format("%.1f", rating)
        ratingBarProfile.rating = rating.toFloat()
        tvRatingCount.text      = if (ratingCount == 0L) "Sin calificaciones aún"
        else "$ratingCount calificación${if (ratingCount > 1) "es" else ""}"

        rvComments.layoutManager            = LinearLayoutManager(this)
        rvComments.isNestedScrollingEnabled = false

        db.collection("calificaciones")
            .whereEqualTo("ratedUserId", session.getUserId())
            .get()
            .addOnSuccessListener { docs ->
                val comments = docs.documents.mapNotNull { doc ->
                    val stars = (doc.getLong("stars") ?: 0L).toInt()
                    if (stars > 0) Comment(
                        ownerName = doc.getString("ownerName") ?: "Usuario",
                        stars     = stars,
                        comment   = doc.getString("comment")   ?: ""
                    ) else null
                }
                if (comments.isNotEmpty())
                    rvComments.adapter = CommentsAdapter(comments)
            }
    }

    private fun processPhoto(uri: Uri) {
        try {
            savePhoto(BitmapFactory.decodeStream(contentResolver.openInputStream(uri)))
        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePhoto(bitmap: Bitmap) {
        val resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
        val baos    = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val b64     = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

        db.collection("usuarios").document(session.getUserId())
            .update("photoBase64", b64)
            .addOnSuccessListener {
                Toast.makeText(this, "✓ Foto actualizada", Toast.LENGTH_SHORT).show()
                loadBase64(b64, findViewById(R.id.ivProfilePhoto), findViewById(R.id.tvProfileInitial))
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar foto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadBase64(b64: String, iv: ImageView, tv: TextView) {
        try {
            val bytes  = Base64.decode(b64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            iv.setImageBitmap(bitmap)
            iv.visibility = View.VISIBLE
            tv.visibility = View.GONE
        } catch (e: Exception) {
            iv.visibility = View.GONE
            tv.visibility = View.VISIBLE
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
                etName.setText(doc.getString("name")       ?: "")
                etPhone.setText(doc.getString("phone")     ?: "")
                etBirthDate.setText(doc.getString("birthDate") ?: "")
                if (session.getUserType() == "walker")
                    etCardNumber.setText(doc.getString("cardNumber") ?: "")
            }

        AlertDialog.Builder(this)
            .setTitle("Editar mis datos")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name      = etName.text.toString().trim()
                val phone     = etPhone.text.toString().trim()
                val birthDate = etBirthDate.text.toString().trim()
                val card      = etCardNumber.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updates = hashMapOf<String, Any>(
                    "name" to name, "phone" to phone, "birthDate" to birthDate
                )
                if (session.getUserType() == "walker" && card.isNotEmpty())
                    updates["cardNumber"] = card

                db.collection("usuarios").document(session.getUserId())
                    .update(updates)
                    .addOnSuccessListener {
                        session.saveSession(session.getUserId(), session.getUserType(), name)
                        Toast.makeText(this, "✓ Datos actualizados", Toast.LENGTH_SHORT).show()
                        recreate()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}