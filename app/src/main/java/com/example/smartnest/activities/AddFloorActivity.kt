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
        
        // Make activity edge-to-edge to remove the bottom navigation bar background
        window.apply {
            clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
            decorView.systemUiVisibility =
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        setContentView(R.layout.activity_add_floor)

        homeId = intent.getStringExtra("homeId")
        val etFloorName = findViewById<EditText>(R.id.etFloorName)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

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
        val defaultItems = listOf(
            SelectableItem("ground", "Ground Floor", R.drawable.ic_floors),
            SelectableItem("first", "First Floor", R.drawable.ic_floors),
            SelectableItem("second", "Second Floor", R.drawable.ic_floors),
            SelectableItem("third", "Third Floor", R.drawable.ic_floors),
            SelectableItem("fourth", "Fourth Floor", R.drawable.ic_floors),
            SelectableItem("basement", "Basement", R.drawable.ic_basement),
            SelectableItem("attic", "Attic", R.drawable.ic_attic),
            SelectableItem("garage", "Garage", R.drawable.ic_garage)
        )

        database.getReference("floorTypes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<SelectableItem>()
                    for (child in snapshot.children) {
                        val id = child.key ?: continue
                        val catalogItem = child.getValue(TypeCatalogItem::class.java) ?: continue
                        if (catalogItem.label.isNotEmpty()) {
                            items.add(
                                SelectableItem(
                                    id = id,
                                    label = catalogItem.label,
                                    iconRes = IconMapper.resolve(catalogItem.icon)
                                )
                            )
                        }
                    }

                    val finalItems = if (items.isNotEmpty()) items else defaultItems
                    adapter = SelectableGridAdapter(finalItems, showLabel = true, selectedIndex = 0) { _, _ -> }
                    rv.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    adapter = SelectableGridAdapter(defaultItems, showLabel = true, selectedIndex = 0) { _, _ -> }
                    rv.adapter = adapter
                }
            })
    }
}