package com.example.lifterlab.data.model

data class EquipmentItem(
    val name: String = "",
    val type: String = "",
    val totalSessionsUsed: Int = 0,
    val maxSessionsThreshold: Int = 50,
    val needsMaintenance: Boolean = false
)
