package com.example.campusconnect

import java.io.Serializable

data class Startup(
    val id: String? = null,
    val title: String? = null,
    val category: String? = null,
    val studentName: String? = null,
    val studentId: String? = null,
    val description: String? = null,
    val status: String? = "pending", // "pending" or "approved"
    val requiredSkills: String? = null,
    val teamSize: String? = null,
    val stage: String? = null,
    val teamMembers: Map<String, TeamMember>? = null
) : Serializable

data class TeamMember(
    val name: String? = null,
    val role: String? = null,
    val uid: String? = null,
    val imageUrl: String? = null
) : Serializable