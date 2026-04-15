package com.example.door

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearMessageListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WearService", "Message received: ${messageEvent.path}")
        
        if (messageEvent.path == "/unlock_phone") {
            unlockDevice()
        }
    }

    private fun unlockDevice() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // Wake up the screen
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "DoorApp::UnlockWakeLock"
        )
        wakeLock.acquire(3000)

        // Start the Activity to handle the unlock process
        val intent = android.content.Intent(this, com.example.door.presentation.MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("dismiss_keyguard", true)
        }
        startActivity(intent)
    }
}
