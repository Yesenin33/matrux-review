package ru.palma.app.session

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address

class NetworkWatch(context: Context) {

    private val manager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val current = MutableStateFlow(label())
    val label: StateFlow<String> = current.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refresh()
        }

        override fun onLost(network: Network) {
            refresh()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            refresh()
        }

        override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) {
            refresh()
        }
    }

    init {
        runCatching {
            manager?.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        }
        refresh()
    }

    private fun refresh() {
        current.value = label()
    }

    private fun label(): String {
        val service = manager ?: return ""
        val network = runCatching { service.activeNetwork }.getOrNull() ?: return "нет сети"
        val capabilities = runCatching { service.getNetworkCapabilities(network) }.getOrNull()
            ?: return "нет сети"
        val kind = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "моб."
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "кабель"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "сеть"
        }
        val address = runCatching {
            service.getLinkProperties(network)?.linkAddresses
                ?.firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }
                ?.address?.hostAddress
        }.getOrNull()
        return if (address != null) "$kind $address" else kind
    }
}
