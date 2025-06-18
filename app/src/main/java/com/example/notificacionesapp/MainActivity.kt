package com.example.notificacionesapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No hay layout, no se llama a setContentView

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("TOKEN", "Error al obtener el token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("TOKEN", "Token del dispositivo: $token")
        }
    }
}

