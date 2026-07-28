package com.example.smartnest.model

sealed class DeviceData {
    abstract val id: String
    abstract val name: String
    abstract val type: String

    data class Light(
        override val id: String,
        override val name: String,
        override val type: String = "light",
        val isOn: Boolean = false,
        val brightness: Int = 75
    ) : DeviceData()

    data class Outlet(
        override val id: String,
        override val name: String,
        override val type: String = "outlet",
        val isOn: Boolean = false
    ) : DeviceData()

    data class MultiSwitch(
        override val id: String,
        override val name: String,
        override val type: String = "multi_switch",
        val switchCount: Int = 2,
        val switches: Map<String, SwitchEntry> = mapOf(
            "switch1" to SwitchEntry("Switch 1", false),
            "switch2" to SwitchEntry("Switch 2", false)
        )
    ) : DeviceData()

    data class HazardAppliance(
        override val id: String,
        override val name: String,
        override val type: String = "hazard_appliance",
        val isOn: Boolean = false,
        val maxRuntimeMinutes: Int = 30,
        val remainingSeconds: Int = 0,
        val timerActive: Boolean = false
    ) : DeviceData()

    data class Camera(
        override val id: String,
        override val name: String,
        override val type: String = "camera",
        val isOn: Boolean = false,
        val lastUpdated: String = "",
        val snapshotUrl: String = ""
    ) : DeviceData()
}

data class SwitchEntry(
    val label: String = "Switch",
    val isOn: Boolean = false
)
