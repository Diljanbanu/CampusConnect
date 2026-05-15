package com.example.campusconnect.model

data class Notification(
    val id: String? = null,
    val userId: String? = null,
    val title: String? = null,
    val body: String? = null,
    val type: String? = null,
    val timestamp: Long? = null,
    val isRead: Boolean = false
)
