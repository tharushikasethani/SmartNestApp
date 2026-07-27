package com.example.smartnest

object IconMapper {
    fun resolve(iconKey: String): Int = when (iconKey.lowercase()) {
        "house_family", "house", "home" -> R.drawable.ic_house_family
        "apartment" -> R.drawable.ic_apartment
        "sofa", "living_room", "livingroom" -> R.drawable.ic_sofa
        "kitchen" -> R.drawable.ic_kitchen
        "bed", "bedroom" -> R.drawable.ic_bed
        "bath", "bathroom" -> R.drawable.ic_bath
        "dining", "dining_room", "diningroom" -> R.drawable.ic_kitchen // fallback
        "lamp", "light" -> R.drawable.ic_lamp
        "fan" -> R.drawable.ic_fan
        "camera" -> R.drawable.ic_camera
        "plug", "smart_plug" -> R.drawable.ic_plug
        "tv" -> R.drawable.ic_tv
        "purifier" -> R.drawable.ic_purifier
        "water_heater", "heater" -> R.drawable.ic_water_heater
        "lock" -> R.drawable.ic_lock
        "floors", "ground", "first", "second" -> R.drawable.ic_floors
        "basement" -> R.drawable.ic_basement
        "attic" -> R.drawable.ic_attic
        "garage" -> R.drawable.ic_garage
        "others", "other" -> R.drawable.ic_grid
        else -> R.drawable.ic_grid
    }
}