package com.example.campusconnect.model

data class Message(
    var messageId: String? = null,
    var senderId: String? = null,
    var receiverId: String? = null,
    var message: String? = null,
    var fileUrl: String? = null,
    var fileName: String? = null,
    var fileSize: String? = null,
    var timestamp: Long? = null,
    var status: Int? = 1,
    var seenAt: Long? = null,
    var type: String? = "text",
    var deleted: Boolean? = false,
    var deletedFor: String? = null,
    var forwarded: Boolean? = false
)
