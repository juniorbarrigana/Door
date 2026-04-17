package com.example.door.presentation

import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if Bluetooth is enabled
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth to unlock door", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Register listener for the phone's confirmation
        Wearable.getMessageClient(this).addListener(this)

        // The Activity starts transparent and executes the logic immediately
        lifecycleScope.launch {
            // 1. Send unlock message to the phone
            sendUnlockMessage()
            
            // We don't call launchHIDApp here anymore. 
            // We wait for the message in onMessageReceived.
            
            // Safety timeout: if phone doesn't respond in 10 seconds, finish anyway
            delay(10000)
            finish()
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/unlock_done") {
            lifecycleScope.launch {
                Log.d("DoorUnlock", "Phone confirmed unlock. Waiting 2 seconds...")
                delay(2000) // The 2-second delay you requested
                launchHIDApp()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
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
