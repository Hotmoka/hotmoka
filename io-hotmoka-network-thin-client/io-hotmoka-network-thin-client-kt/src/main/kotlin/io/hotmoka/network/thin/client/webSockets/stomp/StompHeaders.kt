package io.hotmoka.network.thin.client.webSockets.stomp

import java.util.NoSuchElementException
import kotlin.jvm.Throws

/**
 * A simple class which represents the STOMP headers.
 */
class StompHeaders {
    private val headers: MutableMap<String, String> = mutableMapOf()

    fun add(key: String, value: String) {
        headers[key] = value
    }

    private fun getHeader(key: String): String? {
        return headers[key]
    }

    @Throws(NoSuchElementException::class)
    fun getDestination(): String {
        return getHeader("destination") ?: throw NoSuchElementException("Destination not found")
    }

    override fun toString(): String {
        return headers.toString()
    }
}