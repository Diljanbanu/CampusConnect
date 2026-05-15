package com.example.campusconnect.repository

import com.example.campusconnect.model.Skill
import com.google.firebase.database.*

class SkillRepository {
    private val db = FirebaseDatabase.getInstance().reference.child("Skills")

    fun addSkill(skill: Skill, onComplete: (Boolean) -> Unit) {
        val key = db.push().key ?: run {
            onComplete(false)
            return
        }
        db.child(key).setValue(skill.copy(id = key))
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun getAllSkills(callback: (List<Skill>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Skill::class.java) }
                callback(list)
            }
            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }
        db.addValueEventListener(listener)
        return listener
    }

    fun getStudentSkills(studentId: String, callback: (List<Skill>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Skill::class.java) }
                callback(list)
            }
            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }
        db.orderByChild("studentId").equalTo(studentId).addValueEventListener(listener)
        return listener
    }

    fun updateSkill(skill: Skill, updates: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val id = skill.id ?: run {
            onComplete(false)
            return
        }
        db.child(id).updateChildren(updates)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteSkill(skillId: String, onComplete: (Boolean) -> Unit) {
        db.child(skillId).removeValue()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }
}
