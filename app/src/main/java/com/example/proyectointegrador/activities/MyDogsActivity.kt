package com.example.proyectointegrador.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class MyDogsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_dogs)

        session = SessionManager(this)

        val toolbar    = findViewById<Toolbar>(R.id.toolbar)
        val rvDogs     = findViewById<RecyclerView>(R.id.rvDogs)
        val layoutEmpty= findViewById<View>(R.id.layoutEmpty)
        val btnAddNew  = findViewById<MaterialButton>(R.id.btnAddNewDog)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mis Mascotas"

        rvDogs.layoutManager = LinearLayoutManager(this)

        btnAddNew.setOnClickListener {
            startActivity(Intent(this, RegisterDogActivity::class.java))
        }

        loadDogs(rvDogs, layoutEmpty)
    }

    override fun onResume() {
        super.onResume()
        loadDogs(
            findViewById(R.id.rvDogs),
            findViewById(R.id.layoutEmpty)
        )
    }

    private fun loadDogs(rvDogs: RecyclerView, layoutEmpty: View) {
        db.collection("perros")
            .whereEqualTo("ownerId", session.getUserId())
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    layoutEmpty.visibility = View.VISIBLE
                    rvDogs.visibility      = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    rvDogs.visibility      = View.VISIBLE
                    rvDogs.adapter         = DogsAdapter(docs.documents) { dogId ->
                        // Tocar un perro abre su formulario de edición
                        startActivity(Intent(this, RegisterDogActivity::class.java).apply {
                            putExtra("dog_id", dogId)
                        })
                    }
                }
            }
    }

    inner class DogsAdapter(
        private val dogs: List<com.google.firebase.firestore.DocumentSnapshot>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<DogsAdapter.DogVH>() {

        inner class DogVH(v: View) : RecyclerView.ViewHolder(v) {
            val ivPhoto:   ImageView    = v.findViewById(R.id.ivDogPhoto)
            val tvInitial: TextView     = v.findViewById(R.id.tvDogInitial)
            val tvName:    TextView     = v.findViewById(R.id.tvDogName)
            val tvBreed:   TextView     = v.findViewById(R.id.tvDogBreed)
            val tvSize:    TextView     = v.findViewById(R.id.tvDogSize)
            val tvAge:     TextView     = v.findViewById(R.id.tvDogAge)
            val btnEdit:   MaterialButton = v.findViewById(R.id.btnEditDog)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            DogVH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dog, parent, false))

        override fun getItemCount() = dogs.size

        override fun onBindViewHolder(holder: DogVH, position: Int) {
            val doc   = dogs[position]
            val name  = doc.getString("name")  ?: ""
            val breed = doc.getString("breed") ?: ""
            val size  = doc.getString("size")  ?: ""
            val age   = doc.getLong("age")     ?: 0L
            val photo = doc.getString("photoBase64") ?: ""

            holder.tvName.text  = name
            holder.tvBreed.text = breed
            holder.tvSize.text  = size
            holder.tvAge.text   = "$age años"

            if (photo.isNotEmpty()) {
                try {
                    val bytes  = Base64.decode(photo, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    holder.ivPhoto.setImageBitmap(bitmap)
                    holder.ivPhoto.visibility   = View.VISIBLE
                    holder.tvInitial.visibility = View.GONE
                } catch (e: Exception) {
                    holder.ivPhoto.visibility   = View.GONE
                    holder.tvInitial.visibility = View.VISIBLE
                }
            } else {
                holder.ivPhoto.visibility   = View.GONE
                holder.tvInitial.visibility = View.VISIBLE
            }

            holder.btnEdit.setOnClickListener { onClick(doc.id) }
            holder.itemView.setOnClickListener { onClick(doc.id) }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}