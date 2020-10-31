package io.hotmoka.network.thin.client.webSockets.stomp

import kotlin.jvm.Throws

/**
 * Helper class to parse STOMP messages.
 */
class StompMessageParser(private val stompMessage: String) {
    private val END = "\u0000"
    private val NEW_LINE = "\n"
    private val EMPTY_LINE = ""
    private val DELIMITER = ":"

    /**
     * It parses the current STOMP message and returns a [Message].
     * @return the wrapped STOMP message as [Message]
     */
    @Throws(IllegalStateException::class)
    fun parse(): Message {
        val splitMessage = stompMessage.split(NEW_LINE.toRegex()).toTypedArray()

        if (splitMessage.isEmpty())
            throw IllegalStateException("Did not received any message")

        val command = splitMessage[0]
        val stompHeaders = StompHeaders()
        var body = ""

        var cursor = 1
        for (i in cursor until splitMessage.size) {
            // empty line
            if (splitMessage[i] == EMPTY_LINE) {
                cursor = i
                break
            } else {
                val header: List<String> = splitMessage[i].split(DELIMITER)
                stompHeaders.add(header[0], header[1])
            }
        }

        for (i in cursor until splitMessage.size) {
            body += splitMessage[i]
        }

        return if (body.isNotEmpty())
            Message(StompCommand.valueOf(command), stompHeaders, body.replace(END,""))
        else
            Message(StompCommand.valueOf(command), stompHeaders)
    }

}