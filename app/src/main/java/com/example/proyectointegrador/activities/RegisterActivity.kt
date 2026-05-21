package com.example.proyectointegrador.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var selectedType = "owner"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        session = SessionManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Crear cuenta"

        val rgUserType    = findViewById<RadioGroup>(R.id.rgUserType)
        val etFirstName   = findViewById<TextInputEditText>(R.id.etFirstName)
        val etLastName1   = findViewById<TextInputEditText>(R.id.etLastName1)
        val etLastName2   = findViewById<TextInputEditText>(R.id.etLastName2)
        val etEmail       = findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone       = findViewById<TextInputEditText>(R.id.etPhone)
        val etBirthDate   = findViewById<TextInputEditText>(R.id.etBirthDate)
        val tilBirthDate  = findViewById<TextInputLayout>(R.id.tilBirthDate)
        val etPassword    = findViewById<TextInputEditText>(R.id.etPassword)
        val tilCardNumber = findViewById<TextInputLayout>(R.id.tilCardNumber)
        val etCardNumber  = findViewById<TextInputEditText>(R.id.etCardNumber)
        val btnRegister   = findViewById<MaterialButton>(R.id.btnRegister)
        val tvGoLogin     = findViewById<TextView>(R.id.tvGoLogin)

        // Tipo de usuario
        rgUserType.setOnCheckedChangeListener { _, checkedId ->
            selectedType = if (checkedId == R.id.rbWalker) "walker" else "owner"
            tilCardNumber.visibility =
                if (selectedType == "walker") View.VISIBLE else View.GONE
        }

        // FIX: Formato de teléfono 000-000-0000
        etPhone.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true
                val digits = s.toString().filter { it.isDigit() }
                val formatted = when {
                    digits.length <= 3  -> digits
                    digits.length <= 6  -> "${digits.substring(0,3)}-${digits.substring(3)}"
                    digits.length <= 10 -> "${digits.substring(0,3)}-${digits.substring(3,6)}-${digits.substring(6)}"
                    else -> "${digits.substring(0,3)}-${digits.substring(3,6)}-${digits.substring(6,10)}"
                }
                etPhone.setText(formatted)
                etPhone.setSelection(formatted.length)
                isFormatting = false
            }
        })

        // FIX: Formato tarjeta 0000-0000-0000-0000
        etCardNumber.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true
                val digits = s.toString().filter { it.isDigit() }.take(16)
                val formatted = buildString {
                    digits.forEachIndexed { i, c ->
                        if (i > 0 && i % 4 == 0) append("-")
                        append(c)
                    }
                }
                etCardNumber.setText(formatted)
                etCardNumber.setSelection(formatted.length)
                isFormatting = false
            }
        })

        // FIX: Calendario para fecha de nacimiento
        etBirthDate.setOnClickListener { showDatePicker(etBirthDate) }
        tilBirthDate.setEndIconOnClickListener { showDatePicker(etBirthDate) }

        // Registrar
        btnRegister.setOnClickListener {
            val firstName  = etFirstName.text.toString().trim()
            val lastName1  = etLastName1.text.toString().trim()
            val lastName2  = etLastName2.text.toString().trim()
            val email      = etEmail.text.toString().trim()
            val phone      = etPhone.text.toString().trim()
            val birthDate  = etBirthDate.text.toString().trim()
            val password   = etPassword.text.toString().trim()
            val cardNumber = etCardNumber.text.toString().trim()

            // Validaciones
            var hasError = false

            if (firstName.isEmpty()) {
                findViewById<TextInputLayout>(R.id.tilFirstName).error = "Campo requerido"
                hasError = true
            } else { findViewById<TextInputLayout>(R.id.tilFirstName).error = null }

            if (lastName1.isEmpty()) {
                findViewById<TextInputLayout>(R.id.tilLastName1).error = "Campo requerido"
                hasError = true
            } else { findViewById<TextInputLayout>(R.id.tilLastName1).error = null }

            if (lastName2.isEmpty()) {
                findViewById<TextInputLayout>(R.id.tilLastName2).error = "Campo requerido"
                hasError = true
            } else { findViewById<TextInputLayout>(R.id.tilLastName2).error = null }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                findViewById<TextInputLayout>(R.id.tilEmail).error = "Correo inválido"
                hasError = true
            } else { findViewById<TextInputLayout>(R.id.tilEmail).error = null }

            if (phone.replace("-","").length < 10) {
                findViewById<TextInputLayout>(R.id.tilPhone).error = "Teléfono incompleto"
                hasError = true
            } else { findViewById<TextInputLayout>(R.id.tilPhone).error = null }

            if (birthDate.isEmpty()) {
                tilBirthDate.error = "Selecciona tu fecha de nacimiento"
                hasError = true
            } else { tilBirthDate.error = null }

            if (password.length < 6) {
                findViewById<TextInputLayout>(R.id.tilPassword).error = "Mínimo 6 caracteres"
                hasError = true
            } else { findViewById<TextInputLayout>(R.id.tilPassword).error = null }

            if (selectedType == "walker" && cardNumber.replace("-","").length < 16) {
                tilCardNumber.error = "Tarjeta incompleta (16 dígitos)"
                hasError = true
            } else { tilCardNumber.error = null }

            if (hasError) return@setOnClickListener

            // Nombre completo
            val fullName = "$firstName $lastName1 $lastName2"

            btnRegister.isEnabled = false
            btnRegister.text = "Registrando..."

            db.collection("usuarios")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        Toast.makeText(this,
                            "Este correo ya está registrado",
                            Toast.LENGTH_SHORT).show()
                        btnRegister.isEnabled = true
                        btnRegister.text = "Crear cuenta"
                        return@addOnSuccessListener
                    }

                    val newUser = hashMapOf(
                        "name"        to fullName,
                        "firstName"   to firstName,
                        "lastName1"   to lastName1,
                        "lastName2"   to lastName2,
                        "email"       to email,
                        "phone"       to phone,
                        "birthDate"   to birthDate,
                        "password"    to password,
                        "userType"    to selectedType,
                        "cardNumber"  to cardNumber,
                        "isAvailable" to true,
                        "rating"      to 0.0,
                        "ratingCount" to 0
                    )

                    db.collection("usuarios")
                        .add(newUser)
                        .addOnSuccessListener { documentRef ->
                            Log.d("REGISTER", "Usuario creado: ${documentRef.id}")
                            Toast.makeText(this,
                                "¡Cuenta creada! Ahora inicia sesión",
                                Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this,
                                "Error: ${e.message}",
                                Toast.LENGTH_LONG).show()
                            btnRegister.isEnabled = true
                            btnRegister.text = "Crear cuenta"
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this,
                        "Sin conexión: ${e.message}",
                        Toast.LENGTH_LONG).show()
                    btnRegister.isEnabled = true
                    btnRegister.text = "Crear cuenta"
                }
        }

        tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // FIX: DatePickerDialog para seleccionar fecha
    private fun showDatePicker(etBirthDate: TextInputEditText) {
        val cal = Calendar.getInstance()
        val year  = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day   = cal.get(Calendar.DAY_OF_MONTH)

        val picker = DatePickerDialog(this, { _, y, m, d ->
            val formatted = "%02d/%02d/%04d".format(d, m + 1, y)
            etBirthDate.setText(formatted)
        }, year, month, day)

        // Máximo: hoy (no puede registrarse alguien que aún no nace)
        picker.datePicker.maxDate = cal.timeInMillis

        // Mínimo razonable: hace 100 años
        cal.add(Calendar.YEAR, -100)
        picker.datePicker.minDate = cal.timeInMillis

        picker.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}