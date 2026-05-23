package com.example.campusconnect.model

data class AppVersion(
    val version: String? = "v2.5.1",
    val buildRef: String? = "build-4402688",
    val changelog: List<String>? = listOf(
        "Optimized system navigation pathways",
        "Polished profile statistics synchronization",
        "Added beautiful dynamic custom resume customizer",
        "Interactive Dark Mode state toggle handling"
    )
)
