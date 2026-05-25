package com.example.campusconnect.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SmsSender {

    // These would be replaced with actual API credentials for a service like Twilio or MSG91
    private const val API_KEY = "your_api_key_here"
    private const val SENDER_ID = "CAMPUS"

    fun sendOtpSms(phoneNumber: String, otp: String, callback: (Boolean) -> Unit) {
        val message = "Your CampusConnect verification code is: $otp. Valid for 5 minutes."
        
        // In a real production scenario, you would make an HTTP request to your SMS Gateway API.
        // Below is a structural example of how it would be implemented.
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // This is a placeholder for a real API call. 
                // For demonstration, we simulate a successful API trigger after a short delay.
                
                Log.d("SmsSender", "Simulating SMS delivery to $phoneNumber: $message")
                
                /*
                // Real implementation example:
                val urlString = "https://api.example.com/sms/send?apiKey=$API_KEY&to=$phoneNumber&message=${URLEncoder.encode(message, "UTF-8")}&sender=$SENDER_ID"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    callback(true)
                } else {
                    callback(false)
                }
                */
                
                // For now, since no real API key is provided, we simulate success 
                // so the UI flow can be tested, but we log the attempt clearly.
                Thread.sleep(1000) 
                callback(true)
                
            } catch (e: Exception) {
                Log.e("SmsSender", "Failed to send SMS: ${e.message}")
                callback(false)
            }
        }
    }
}
