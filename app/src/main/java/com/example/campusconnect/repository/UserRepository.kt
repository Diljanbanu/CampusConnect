package com.example.campusconnect.repository

import com.example.campusconnect.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference.child("Users")

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getUserProfile(userId: String, callback: (User?) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.getValue(User::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        }
        db.child(userId).addValueEventListener(listener)
        return listener
    }

    fun updateProfile(userId: String, updates: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        db.child(userId).updateChildren(updates)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun logout() {
        auth.signOut()
    }
}
