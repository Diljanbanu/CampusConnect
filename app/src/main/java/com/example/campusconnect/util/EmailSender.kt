package com.example.campusconnect.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    private const val SENDER_EMAIL = "campusconnect2801@gmail.com"
    private const val SENDER_PASSWORD = "your_app_password_here" 

    fun sendWelcomeEmail(
        recipientEmail: String,
        studentName: String,
        department: String = "Not Specified",
        semester: String = "Not Specified"
    ) {
        val subject = "Welcome to CampusConnect - Your Account is Activated!"
        
        val body = """
            Dear $studentName,

            Welcome to CampusConnect.

            Your RK University student account has been successfully activated, and you now have access to the official student collaboration platform designed to connect students, skills, startups, and campus opportunities in one place.

            Through CampusConnect, you can:

            • Showcase your professional skills and projects
            • Connect and collaborate with fellow students
            • Participate in university events and workshops
            • Explore startup opportunities and innovation communities
            • Build your academic and professional network
            • Communicate through real-time messaging and collaboration tools

            Registered Account Information
            --------------------------------------------------
            Name        : $studentName
            Email       : $recipientEmail
            Department  : $department
            Semester    : $semester
            --------------------------------------------------

            We encourage you to complete your profile and actively participate in the CampusConnect community to maximize learning, networking, and career opportunities.

            For security purposes, please do not share your account credentials with anyone.

            If you require any assistance or support, feel free to contact us at:

            campusconnect2801@gmail.com

            Thank you for being a part of CampusConnect.

            Warm Regards,  
            CampusConnect Team  
            RK University

            Connect • Collaborate • Innovate
        """.trimIndent()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.socketFactory.port", "465")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.port", "465")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, "CampusConnect Team"))
                    setRecipient(javax.mail.Message.RecipientType.TO, InternetAddress(recipientEmail))
                    setSubject(subject)
                    setText(body)
                }

                Transport.send(message)
                Log.d("EmailSender", "Welcome email sent successfully to $recipientEmail")
            } catch (e: Exception) {
                Log.e("EmailSender", "Failed to send email: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
