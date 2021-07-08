package io.hotmoka.network.thin.client.webSockets.stomp

import com.google.gson.Gson


class StompMessageBuilder {

    /**
     * Builder class which builds the STOMP messages by command. The following "commands" are implemented:
     *
     *  - connect to webSocket
     *  - subscribe to a topic
     *  - unsubscribe from a topic
     *  - send payload to a destination
     *
     */
   companion object {
       private val gson = Gson()
       private val END = "\u0000"
       private val NEW_LINE = "\n"
       private val DESTINATION = "destination"
       private val ID = "id"
       private val ACK = "ack"
       private val RECEIPT = "receipt"

       fun buildSubscribeMessage(destination: String, id: String): String {
           var headers = ""

           headers += buildHeader(StompCommand.SUBSCRIBE.name)
           headers += buildHeader(DESTINATION, destination)
           headers += buildHeader(ID, id)
           headers += buildHeader(ACK, "auto")
           headers += buildHeader(RECEIPT, "receipt_$destination")

           return "$headers$NEW_LINE$END"
       }

       fun buildUnsubscribeMessage(subscriptionId: String): String {
           var headers = ""

           headers += buildHeader(StompCommand.UNSUBSCRIBE.name)
           headers += buildHeader(ID, subscriptionId)

           return "$headers$NEW_LINE$END"
       }

       fun buildConnectMessage(): String {
           var headers = ""

           headers += buildHeader(StompCommand.CONNECT.name)
           headers += buildHeader("accept-version", "1.0,1.1,2.0")
           headers += buildHeader("host", "stomp.github.org")
           headers += buildHeader("heart-beat", "0,0")

           return "$headers$NEW_LINE$END"
       }

       fun <T> buildSendMessage(destination: String, payload: T?): String {
           val body = if (payload != null) gson.toJson(payload) else ""
           var headers = ""

           headers += buildHeader(StompCommand.SEND.name)
           headers += buildHeader(DESTINATION, destination)

           return "$headers$NEW_LINE$body$NEW_LINE$END"
       }

       private fun buildHeader(key: String, value: String? = null): String {
           return if (value != null) "$key:$value$NEW_LINE" else "$key$NEW_LINE"
       }
   }
}