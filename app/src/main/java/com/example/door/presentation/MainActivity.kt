package com.example.door.presentation

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if Bluetooth is enabled
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth to unlock door", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // The Activity starts transparent and executes the logic immediately
        lifecycleScope.launch {
            // 1. Send unlock message to the phone
            sendUnlockMessage()
            
            // 2. Try to open the HID app
            launchHIDApp()
            
            // 3. Finish this app to avoid staying in background
            finish()
        }
    }

    private suspend fun sendUnlockMessage() {
        withContext(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(this@MainActivity)
                val messageClient = Wearable.getMessageClient(this@MainActivity)
                val nodes = nodeClient.connectedNodes.await()

                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/unlock_phone", "unlock".toByteArray()).await()
                    Log.d("DoorUnlock", "Message sent to: ${node.displayName}")
                }
            } catch (e: Exception) {
                Log.e("DoorUnlock", "Error sending message", e)
            }
        }
    }

    private fun launchHIDApp() {
        try {
            val pm = packageManager
            val packages = pm.getInstalledPackages(0)
            
            // Search for the first app containing "hid" in the package name
            val hidPackage = packages.firstOrNull { 
                it.packageName.contains("hid", ignoreCase = true) && 
                it.packageName != packageName 
            }
            
            if (hidPackage != null) {
                val intent = pm.getLaunchIntentForPackage(hidPackage.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    Log.d("DoorUnlock", "Launching HID: ${hidPackage.packageName}")
                }
            } else {
                Log.e("DoorUnlock", "HID app not found on the watch")
            }
        } catch (e: Exception) {
            Log.e("DoorUnlock", "Error launching HID", e)
        }
    }
}
