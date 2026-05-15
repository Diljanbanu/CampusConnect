package com.example.campusconnect

data class Notification(
    val id: String? = null,
    val userId: String? = null, // Target user
    val title: String? = null,
    val body: String? = null,
    val type: String? = null, // "skill", "event", "startup", "chat"
    val timestamp: Long? = null,
    val isRead: Boolean = false
)
