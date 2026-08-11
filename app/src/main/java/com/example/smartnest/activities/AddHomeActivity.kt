package com.example.smartnest.activities

import android.os.Bundle
import android.widget.EditText
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot

class AddHomeActivity : AppCompatActivity() {

    private lateinit var adapter: SelectableGridAdapter
    private lateinit var etHomeName: EditText
    private lateinit var etHomeAddress: EditText
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

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

        setContentView(R.layout.activity_add_home)

        etHomeName = findViewById(R.id.etHomeName)
        etHomeAddress = findViewById(R.id.etHomeAddress)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvHomeTypes)
        rv.layoutManager = GridLayoutManager(this, 3)

        loadHomeTypes(rv)

        findViewById<TextView>(R.id.btnSave).setOnClickListener {
            saveHome()
        }
    }

    private fun loadHomeTypes(rv: RecyclerView) {
        // We will start with standard defaults so the user always sees something
        val defaultItems = listOf(
            SelectableItem("house", "Family House", R.drawable.ic_house_family),
            SelectableItem("apartment", "Apartment", R.drawable.ic_apartment),
            SelectableItem("villa", "Villa", R.drawable.ic_villa),
            SelectableItem("condominium", "Condo", R.drawable.ic_condominium),
            SelectableItem("townhouse", "Townhouse", R.drawable.ic_townhouse),
            SelectableItem("bungalow", "Bungalow", R.drawable.ic_cottage),
            SelectableItem("duplex", "Duplex", R.drawable.ic_duplex),
            SelectableItem("hostel", "Hostel", R.drawable.ic_hostel),
            SelectableItem("Other", "Other", R.drawable.ic_others),
        )
        
        database.getReference("homeTypes")
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

                    // If database provides types, use them. Otherwise use defaults.
                    val finalItems = if (items.isNotEmpty()) items else defaultItems
                    
                    adapter = SelectableGridAdapter(finalItems, selectedIndex = 0) { _, _ -> }
                    rv.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    adapter = SelectableGridAdapter(defaultItems, selectedIndex = 0) { _, _ -> }
                    rv.adapter = adapter
                }
            })
    }

    private fun saveHome() {
        val name = etHomeName.text.toString().trim()
        val address = etHomeAddress.text.toString().trim()
        val uid = auth.currentUser?.uid

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a home name", Toast.LENGTH_SHORT).show()
            return
        }
        if (uid == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }
        if (!::adapter.isInitialized) {
            Toast.makeText(this, "Home types still loading...", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedType = adapter.getSelected()
        val homeRef = database.getReference("users").child(uid).child("homes").push()

        val homeData = mapOf(
            "name" to name,
            "address" to address,
            "type" to (selectedType?.id ?: "house")
        )

        homeRef.setValue(homeData)
            .addOnSuccessListener {
                Toast.makeText(this, "Home \"$name\" saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}