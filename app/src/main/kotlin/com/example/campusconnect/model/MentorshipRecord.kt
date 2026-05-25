package com.example.campusconnect.model

data class MentorshipRecord(
    val id: String? = null,
    val mentorId: String? = null,
    val studentId: String? = null,
    val studentName: String? = null,
    val skillId: String? = null,
    val skillName: String? = null,
    val date: Long? = null,
    val rating: Float = 0f
)
