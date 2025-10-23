package com.elfak.ecospot

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.SetOptions

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Poruka primljena!")

        remoteMessage.notification?.let {
            val title = it.title
            val body = it.body


            if (title != null && body != null) {
                Log.d("FCM", "Title: $title, Body: $body")
                NotificationHelper.showNotification(
                    applicationContext,
                    title,
                    body
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Novi token: $token")

        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {

        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            val userDocRef = FirebaseFirestore.getInstance().collection("users").document(userId)
            val tokenData = hashMapOf("fcmToken" to token)


            userDocRef.set(tokenData, SetOptions.merge())
                .addOnSuccessListener { Log.d("FCM", "Token uspešno sačuvan/ažuriran.") }
                .addOnFailureListener { e -> Log.w("FCM", "Greška pri čuvanju tokena", e) }
        }
    }
}