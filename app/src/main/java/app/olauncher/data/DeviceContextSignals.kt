package app.olauncher.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

data class WifiNetwork(val bssid: String, val name: String)

object DeviceContextSignals {
    fun matchesHomeWifi(current: WifiNetwork?, savedNames: Set<String>, savedBssids: Set<String>): Boolean {
        if (current == null) return false
        return savedNames.any { it.isNotBlank() && it == current.name } ||
            savedBssids.any { it.isNotBlank() && it.equals(current.bssid, ignoreCase = true) }
    }

    fun currentWifi(context: Context): WifiNetwork? {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val capabilityInfos = runCatching {
            buildList {
                manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }
                    ?.takeIf { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
                    ?.transportInfo?.let { add(it as? WifiInfo) }
                manager.allNetworks.forEach { network ->
                    manager.getNetworkCapabilities(network)
                        ?.takeIf { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
                        ?.transportInfo?.let { add(it as? WifiInfo) }
                }
            }.filterNotNull()
        }.getOrDefault(emptyList())
        @Suppress("DEPRECATION")
        val legacyInfo = runCatching { wifiManager.connectionInfo }.getOrNull()
        return (capabilityInfos + listOfNotNull(legacyInfo)).firstNotNullOfOrNull(::readableWifi)
    }

    fun hasWifiConnection(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return runCatching {
            manager.allNetworks.any { network ->
                manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
        }.getOrDefault(false)
    }

    private fun readableWifi(info: WifiInfo): WifiNetwork? {
        val bssid = info.bssid?.takeUnless { it == "02:00:00:00:00:00" }.orEmpty()
        val name = info.ssid?.removeSurrounding("\"")?.takeUnless { it == WifiManager.UNKNOWN_SSID }.orEmpty()
        if (bssid.isBlank() && name.isBlank()) return null
        return WifiNetwork(bssid, name)
    }

    fun isCarBluetoothConnected(context: Context, address: String, result: (Boolean) -> Unit) {
        if (address.isBlank() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            result(false)
            return
        }
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null || !adapter.isEnabled) {
            result(false)
            return
        }
        fun query(profile: Int, fallback: () -> Unit) {
            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profileType: Int, proxy: BluetoothProfile) {
                    val connected = proxy.connectedDevices.any { it.address.equals(address, ignoreCase = true) }
                    adapter.closeProfileProxy(profileType, proxy)
                    if (connected) result(true) else fallback()
                }
                override fun onServiceDisconnected(profile: Int) = Unit
            }
            if (!adapter.getProfileProxy(context, listener, profile)) fallback()
        }
        query(BluetoothProfile.A2DP) {
            query(BluetoothProfile.HEADSET) { result(false) }
        }
    }
}
