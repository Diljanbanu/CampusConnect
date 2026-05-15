package com.example.campusconnect.model

data class Skill(
    val id: String? = null,
    val skillName: String? = null,
    val studentName: String? = null,
    val studentMobile: String? = null,
    val studentEmail: String? = null,
    val studentId: String? = null,
    val description: String? = null,
    val rating: Float = 0f,
    val reviewsCount: Int = 0,
    val category: String? = null,
    val status: String? = "pending"
)
