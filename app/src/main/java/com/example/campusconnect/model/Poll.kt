package com.example.campusconnect.model

data class Poll(
    val id: String? = null,
    val question: String? = null,
    val options: List<PollOption> = emptyList(),
    val totalVotes: Int = 0,
    val active: Boolean = true
)

data class PollOption(
    val title: String? = null,
    val votes: Int = 0,
    val isCorrect: Boolean = false
)
