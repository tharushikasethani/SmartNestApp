package com.example.smartnest.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.IconMapper
import com.example.smartnest.R
import com.example.smartnest.adapter.SelectableGridAdapter
import com.example.smartnest.model.SelectableItem
import com.example.smartnest.model.TypeCatalogItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddFloorActivity : AppCompatActivity() {

    private lateinit var adapter: SelectableGridAdapter
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var homeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_floor)

        homeId = intent.getStringExtra("homeId")
        val etFloorName = findViewById<EditText>(R.id.etFloorName)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvFloorTypes)
        rv.layoutManager = GridLayoutManager(this, 3)
        loadFloorTypes(rv)

        findViewById<FrameLayout>(R.id.uploadBox).setOnClickListener {
            Toast.makeText(this, "Image picker not wired up yet", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btnSave).setOnClickListener {
            val name = etFloorName.text.toString().trim()
            val uid = auth.currentUser?.uid

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a floor name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (uid == null || homeId == null) {
                Toast.makeText(this, "Missing user or home info", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!::adapter.isInitialized) {
                Toast.makeText(this, "Floor types still loading, try again", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedType = adapter.getSelected()
            val floorRef = database.getReference("users").child(uid).child("homes").child(homeId!!).child("floors").push()

            val floorData = mapOf(
                "name" to name,
                "type" to (selectedType?.id ?: "ground")
            )

            floorRef.setValue(floorData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Floor \"$name\" saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadFloorTypes(rv: RecyclerView) {
        database.getReference("floorTypes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<SelectableItem>()
                    for (child in snapshot.children) {
                        val id = child.key ?: continue
                        val catalogItem = child.getValue(TypeCatalogItem::class.java) ?: continue
                        items.add(
                            SelectableItem(
                                id = id,
                                label = catalogItem.label,
                                iconRes = IconMapper.resolve(catalogItem.icon)
                            )
                        )
                    }

                    if (items.isEmpty()) {
                        items.addAll(listOf(
                            SelectableItem("ground", "Ground Floor", R.drawable.ic_floors),
                            SelectableItem("first", "First Floor", R.drawable.ic_floors),
                            SelectableItem("second", "Second Floor", R.drawable.ic_floors)
                        ))
                    }

                    adapter = SelectableGridAdapter(items, selectedIndex = 0) { _, _ -> }
                    rv.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddFloorActivity, "Failed to load floor types: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}