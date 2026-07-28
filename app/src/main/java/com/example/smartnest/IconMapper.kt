package com.example.smartnest

object IconMapper {

    fun resolve(iconKey: String): Int {
        return when (iconKey.lowercase().trim()) {

            // ==========================
            // Home Types
            // ==========================
            "house", "home", "house_family" ->
                R.drawable.ic_house_family

            "apartment" ->
                R.drawable.ic_apartment

            "villa" ->
                R.drawable.ic_villa

            "condominium", "condo", "domain" ->
                R.drawable.ic_condominium

            "townhouse", "holiday_village" ->
                R.drawable.ic_townhouse

            "bungalow", "cottage" ->
                R.drawable.ic_cottage

            "duplex" ->
                R.drawable.ic_duplex

            "hostel", "hotel" ->
                R.drawable.ic_hostel

// ==========================
// Rooms
// ==========================
            "porch" ->
                R.drawable.ic_porch

            "living_room", "livingroom", "sofa" ->
                R.drawable.ic_sofa

            "media_room", "mediaroom" ->
                R.drawable.ic_media_room

            "bedroom", "bed_room", "bed" ->
                R.drawable.ic_bed

            "primary_suite", "master_bedroom", "master_suite" ->
                R.drawable.ic_primary_suite

            "dining_room", "diningroom", "dining" ->
                R.drawable.ic_dining_room

            "kitchen" ->
                R.drawable.ic_kitchen

            "bathroom", "bath" ->
                R.drawable.ic_bath

            "laundry", "laundry_room" ->
                R.drawable.ic_laundry

            "others", "other" ->
                R.drawable.ic_grid

// ==========================
// Smart Devices
// ==========================

            "light", "lamp" ->
                R.drawable.ic_light

            "blinds" ->
                R.drawable.ic_blinds

            "tv" ->
                R.drawable.ic_tv

            "deck_camera", "camera" ->
                R.drawable.ic_camera

            "refrigerator", "fridge" ->
                R.drawable.ic_refrigerator

            "kitchen_oven", "oven" ->
                R.drawable.ic_oven

            "ceiling_fan", "fan" ->
                R.drawable.ic_fan

            "smart_plug", "plug", "outlet" ->
                R.drawable.ic_plug

            "multi_switch" ->
                R.drawable.ic_switch_multi

            "temperature_sensor", "temp_sensor" ->
                R.drawable.ic_temperature_sensor

            "speaker" ->
                R.drawable.ic_speaker

            "door_lock", "lock" ->
                R.drawable.ic_lock

            "washing_machine", "washer" ->
                R.drawable.ic_washing_machine

            "iron", "hazard_appliance" ->
                R.drawable.ic_iron

            "bathroom_heater", "heater", "water_heater" ->
                R.drawable.ic_bathroom_heater

            // ==========================
            // Floors
            // ==========================
            "ground", "first", "second", "floors", "third", "fourth" ->
                R.drawable.ic_floors

            "basement" ->
                R.drawable.ic_basement

            "attic" ->
                R.drawable.ic_attic

            "garage" ->
                R.drawable.ic_garage

            // ==========================
            // Others
            // ==========================
            "other", "others", "category" ->
                R.drawable.ic_grid

            else ->
                R.drawable.ic_grid
        }
    }
}