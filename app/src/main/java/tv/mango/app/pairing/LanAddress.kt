package tv.mango.app.pairing

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * This device's own address on whatever local network it is attached to.
 *
 * Read from the interfaces directly rather than through [android.net.wifi.WifiManager],
 * which only knows about WiFi: a Fire Stick reached over Ethernet - a common
 * setup, since it frees the one WiFi radio from also driving 4K video - would
 * otherwise have no address to hand a phone at all.
 */
object LanAddress {

    /** The first non-loopback IPv4 address found on an interface that is up, or null if there is none. */
    fun find(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull()
}
