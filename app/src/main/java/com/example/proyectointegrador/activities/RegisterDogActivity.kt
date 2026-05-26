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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class RegisterDogActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var dogPhotoBase64 = ""
    private var selectedGender = "Macho"
    private var editingDogId   = ""  // si viene de edición

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) processPhoto(uri)
            else (result.data?.extras?.get("data") as? Bitmap)?.let { saveBitmap(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_dog)

        session    = SessionManager(this)
        editingDogId = intent.getStringExtra("dog_id") ?: ""

        val toolbar         = findViewById<Toolbar>(R.id.toolbar)
        val ivDogPhoto      = findViewById<ImageView>(R.id.ivDogPhoto)
        val tvDogInitial    = findViewById<TextView>(R.id.tvDogInitial)
        val btnChangeDogPhoto = findViewById<MaterialButton>(R.id.btnChangeDogPhoto)
        val etDogName       = findViewById<TextInputEditText>(R.id.etDogName)
        val etBreed         = findViewById<TextInputEditText>(R.id.etBreed)
        val etAge           = findViewById<TextInputEditText>(R.id.etAge)
        val etAllergy       = findViewById<TextInputEditText>(R.id.etAllergy)
        val chipGroupSize   = findViewById<ChipGroup>(R.id.chipGroupSize)
        val btnGenderMale   = findViewById<MaterialButton>(R.id.btnGenderMale)
        val btnGenderFemale = findViewById<MaterialButton>(R.id.btnGenderFemale)
        val btnSaveDog      = findViewById<MaterialButton>(R.id.btnSaveDog)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (editingDogId.isNotEmpty()) "Editar Mascota" else "Nueva Mascota"

        // Si viene de edición, cargar datos existentes
        if (editingDogId.isNotEmpty()) {
            db.collection("perros").document(editingDogId).get()
                .addOnSuccessListener { doc ->
                    etDogName.setText(doc.getString("name")   ?: "")
                    etBreed.setText(doc.getString("breed")    ?: "")
                    etAge.setText((doc.getLong("age") ?: 0L).toString())
                    etAllergy.setText(doc.getString("allergy") ?: "")
                    selectedGender = doc.getString("gender") ?: "Macho"

                    when (doc.getString("size")) {
                        "Pequeño" -> chipGroupSize.check(R.id.chipSmall)
                        "Mediano" -> chipGroupSize.check(R.id.chipMedium)
                        "Grande"  -> chipGroupSize.check(R.id.chipLarge)
                    }

                    val photo = doc.getString("photoBase64") ?: ""
                    if (photo.isNotEmpty()) {
                        dogPhotoBase64 = photo
                        loadPhoto(photo, ivDogPhoto, tvDogInitial)
                    }

                    updateGenderUI(btnGenderMale, btnGenderFemale, selectedGender)
                }
        }

        // Foto
        btnChangeDogPhoto.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Foto de la mascota")
                .setItems(arrayOf("📷 Tomar foto", "🖼️ Elegir de galería")) { _, which ->
                    when (which) {
                        0 -> pickImageLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                        1 -> pickImageLauncher.launch(
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                .also { it.type = "image/*" }
                        )
                    }
                }.show()
        }

        // Género
        btnGenderMale.setOnClickListener {
            selectedGender = "Macho"
            updateGenderUI(btnGenderMale, btnGenderFemale, "Macho")
        }
        btnGenderFemale.setOnClickListener {
            selectedGender = "Hembra"
            updateGenderUI(btnGenderMale, btnGenderFemale, "Hembra")
        }

        // Guardar
        btnSaveDog.setOnClickListener {
            val dogName = etDogName.text.toString().trim()
            val breed   = etBreed.text.toString().trim()
            val ageStr  = etAge.text.toString().trim()
            val allergy = etAllergy.text.toString().trim().ifEmpty { "Ninguna" }
            val size    = when (chipGroupSize.checkedChipId) {
                R.id.chipSmall  -> "Pequeño"
                R.id.chipMedium -> "Mediano"
                R.id.chipLarge  -> "Grande"
                else            -> ""
            }

            if (dogName.isEmpty() || breed.isEmpty() || size.isEmpty()) {
                Toast.makeText(this, "Completa nombre, raza y tamaño", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val age = ageStr.toIntOrNull() ?: 1

            btnSaveDog.isEnabled = false
            btnSaveDog.text      = "Guardando..."

            val data = hashMapOf<String, Any>(
                "ownerId"     to session.getUserId(),
                "name"        to dogName,
                "breed"       to breed,
                "size"        to size,
                "age"         to age,
                "gender"      to selectedGender,
                "allergy"     to allergy,
                "photoBase64" to dogPhotoBase64
            )

            val task = if (editingDogId.isNotEmpty())
                db.collection("perros").document(editingDogId).set(data)
            else
                db.collection("perros").add(data).continueWith { }

            task.addOnSuccessListener {
                val msg = if (editingDogId.isNotEmpty())
                    "✓ Mascota actualizada" else "✓ ¡$dogName registrado! 🐾"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                finish()
            }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar: ${it.message}", Toast.LENGTH_LONG).show()
                    btnSaveDog.isEnabled = true
                    btnSaveDog.text      = "GUARDAR MASCOTA"
                }
        }
    }

    private fun updateGenderUI(btnMale: MaterialButton, btnFemale: MaterialButton, gender: String) {
        if (gender == "Macho") {
            btnMale.backgroundTintList   = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#F5A623"))
            btnMale.setTextColor(android.graphics.Color.WHITE)
            btnFemale.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#EEEEEE"))
            btnFemale.setTextColor(android.graphics.Color.parseColor("#777777"))
        } else {
            btnFemale.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#F5A623"))
            btnFemale.setTextColor(android.graphics.Color.WHITE)
            btnMale.backgroundTintList   = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#EEEEEE"))
            btnMale.setTextColor(android.graphics.Color.parseColor("#777777"))
        }
    }

    private fun processPhoto(uri: Uri) {
        try {
            saveBitmap(BitmapFactory.decodeStream(contentResolver.openInputStream(uri)))
        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
        val baos    = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        dogPhotoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        val ivDogPhoto   = findViewById<ImageView>(R.id.ivDogPhoto)
        val tvDogInitial = findViewById<TextView>(R.id.tvDogInitial)
        ivDogPhoto.setImageBitmap(resized)
        ivDogPhoto.visibility   = View.VISIBLE
        tvDogInitial.visibility = View.GONE
        Toast.makeText(this, "✓ Foto agregada", Toast.LENGTH_SHORT).show()
    }

    private fun loadPhoto(base64: String, iv: ImageView, tv: TextView) {
        try {
            val bytes  = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            iv.setImageBitmap(bitmap)
            iv.visibility = View.VISIBLE
            tv.visibility = View.GONE
        } catch (e: Exception) {
            iv.visibility = View.GONE
            tv.visibility = View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}