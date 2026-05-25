package com.example.campusconnect.model

data class PrivacyPolicy(
    val sections: List<PolicySection>? = null
)

data class PolicySection(
    val title: String? = null,
    val content: String? = null
)
