package com.example.campusconnect.model

import java.io.Serializable

data class Event(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val date: String? = null,
    val time: String? = null,
    val location: String? = null,
    val category: String? = null,
    val status: String? = "approved",
    val eventStatus: String? = "UPCOMING",
    val imageUrl: String? = null,
    val capacity: Int = 0,
    val registeredCount: Int = 0,
    val isFeatured: Boolean = false,
    val type: String? = "Technical",
    val highlights: List<String>? = null,
    val organizerName: String? = "RKU ACM Student Chapter",
    val organizerEmail: String? = "contact@rku.ac.in",
    val organizerLogoUrl: String? = null
) : Serializable
