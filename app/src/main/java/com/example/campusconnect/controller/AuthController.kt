package com.example.campusconnect.controller

import com.example.campusconnect.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.FirebaseDatabase

class AuthController(private val listener: AuthListener) {

    interface AuthListener {
        fun onAuthSuccess()
        fun onAuthFailure(message: String)
        fun onRegistrationSuccess(message: String)
        fun onEmailVerificationSent(email: String)
    }

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    fun loginOrRegister(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        listener.onAuthSuccess()
                    } else {
                        auth.signOut()
                        listener.onAuthFailure("Please verify your email first.")
                    }
                } else {
                    if (task.exception is FirebaseAuthInvalidUserException) {
                        register(email, pass)
                    } else {
                        listener.onAuthFailure(task.exception?.message ?: "Login Failed")
                    }
                }
            }
    }

    private fun register(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val userId = firebaseUser?.uid
                    
                    firebaseUser?.sendEmailVerification()?.addOnCompleteListener {
                        if (it.isSuccessful) listener.onEmailVerificationSent(email)
                    }

                    val user = User(uid = userId, fullName = "RKU Student", email = email)
                    if (userId != null) {
                        database.reference.child("Users").child(userId).setValue(user)
                            .addOnSuccessListener {
                                listener.onRegistrationSuccess("Registration successful. Please verify email and login.")
                                auth.signOut()
                            }
                    }
                } else {
                    listener.onAuthFailure(task.exception?.message ?: "Registration Failed")
                }
            }
    }
}
