package com.example.campusconnect.repository

import com.example.campusconnect.Event
import com.example.campusconnect.Skill
import com.example.campusconnect.Startup
import com.example.campusconnect.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object DataRepository {
    private val database = FirebaseDatabase.getInstance()

    var currentUser: User? = null
    val allApprovedSkills = mutableListOf<Skill>()
    val allApprovedEvents = mutableListOf<Event>()
    val allApprovedStartups = mutableListOf<Startup>()
    var isDataLoaded = false

    fun prefetchData(onComplete: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onComplete()
            return
        }

        // Fetch Current User
        database.reference.child("Users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
                fetchAllCollections(onComplete)
            }
            override fun onCancelled(error: DatabaseError) {
                fetchAllCollections(onComplete)
            }
        })
    }

    private fun fetchAllCollections(onComplete: () -> Unit) {
        var count = 0
        val total = 3

        val checkComplete = {
            count++
            if (count == total) {
                isDataLoaded = true
                onComplete()
            }
        }

        // Fetch Approved Skills
        database.reference.child("Skills").orderByChild("status").equalTo("approved")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allApprovedSkills.clear()
                    for (ds in snapshot.children) {
                        ds.getValue(Skill::class.java)?.let { allApprovedSkills.add(it) }
                    }
                    checkComplete()
                }
                override fun onCancelled(error: DatabaseError) { checkComplete() }
            })

        // Fetch Approved Events
        database.reference.child("Events").orderByChild("status").equalTo("approved")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allApprovedEvents.clear()
                    for (ds in snapshot.children) {
                        ds.getValue(Event::class.java)?.let { allApprovedEvents.add(it) }
                    }
                    checkComplete()
                }
                override fun onCancelled(error: DatabaseError) { checkComplete() }
            })

        // Fetch Approved Startups
        database.reference.child("Ideas").orderByChild("status").equalTo("approved")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allApprovedStartups.clear()
                    for (ds in snapshot.children) {
                        ds.getValue(Startup::class.java)?.let { allApprovedStartups.add(it) }
                    }
                    checkComplete()
                }
                override fun onCancelled(error: DatabaseError) { checkComplete() }
            })
    }
}
