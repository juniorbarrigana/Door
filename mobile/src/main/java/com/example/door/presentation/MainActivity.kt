package com.example.door.presentation

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se a Intent vier do WearMessageListenerService, tentamos desbloquear
        if (intent.getBooleanExtra("dismiss_keyguard", false)) {
            dismissKeyguard()
        } else {
            // Se for aberta manualmente, apenas fechamos para manter o comportamento "sem interface"
            finish()
        }
    }

    private fun dismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    finish()
                }

                override fun onDismissCancelled() {
                    super.onDismissCancelled()
                    finish()
                }

                override fun onDismissError() {
                    super.onDismissError()
                    finish()
                }
            })
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
            // Para versões antigas, damos um pequeno delay antes de fechar para garantir que os flags funcionem
            window.decorView.postDelayed({ finish() }, 500)
        }
    }
}
