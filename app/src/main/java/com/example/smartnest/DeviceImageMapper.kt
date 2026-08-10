package com.example.smartnest

object DeviceImageMapper {

    fun resolve(deviceType: String?, isOn: Boolean = false): Int {
        val key = (deviceType ?: "").lowercase().trim()

        return when {
            // Camera
            key.contains("camera") || key.contains("cctv") ->
                R.drawable.ic_camera1

            // Light / Lamp
            key.contains("light") || key.contains("lamp") -> {
                if (isOn) {
                    R.drawable.ic_lamp_on
                } else {
                    R.drawable.ic_lamp1
                }
            }

            // Fan
            key.contains("fan") -> R.drawable.ic_fan

            // Air Conditioner
            key.contains("ac") || key.contains("air") ->
                R.drawable.ic_ac

            // Refrigerator
            key.contains("fridge") || key.contains("refrigerator") ->
                R.drawable.ic_refrigerator

            // Washing Machine
            key.contains("washer") || key.contains("washing") ->
                R.drawable.ic_washing_machine

            // Iron
            key.contains("iron") ->
                R.drawable.ic_iron

            // Heater
            key.contains("heater") ->
                R.drawable.ic_bathroom_heater

            // Smart Plug
            key.contains("plug") ||
                    key.contains("outlet") ||
                    key.contains("smart_plug") ->
                R.drawable.ic_plug

            // TV
            key.contains("tv") ->
                R.drawable.ic_tv

            // Blinds
            key.contains("blind") ->
                R.drawable.ic_blinds

            // Oven
            key.contains("oven") ->
                R.drawable.ic_oven

            // Speaker
            key.contains("speaker") ->
                R.drawable.ic_speaker

            // Door Lock
            key.contains("lock") ->
                R.drawable.ic_lock

            // Temperature Sensor
            key.contains("temp") || key.contains("sensor") ->
                R.drawable.ic_temperature_sensor

            // Default
            else ->
                R.drawable.ic_camera1
        }
    }
}