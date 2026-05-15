package com.example.campusconnect.util

import com.example.campusconnect.model.Notification
import com.google.firebase.database.FirebaseDatabase

object NotificationHelper {

    fun sendNotification(userId: String, title: String, body: String, type: String) {
        val database = FirebaseDatabase.getInstance().reference
        
        database.child("Users").child(userId).child("notificationsEnabled").get().addOnSuccessListener { snapshot ->
            val isEnabled = snapshot.value as? Boolean ?: true
            if (isEnabled) {
                val notificationId = database.child("Notifications").child(userId).push().key ?: return@addOnSuccessListener
                
                val notification = Notification(
                    id = notificationId,
                    userId = userId,
                    title = title,
                    body = body,
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
                
                database.child("Notifications").child(userId).child(notificationId).setValue(notification)
            }
        }
    }
}
