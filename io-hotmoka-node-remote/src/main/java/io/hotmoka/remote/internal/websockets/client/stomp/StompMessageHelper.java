/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.remote.internal.websockets.client.stomp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.hotmoka.remote.internal.websockets.client.Message;

/**
 * RemoteNodeConfigBuilderImpl class which builds the STOMP messages by command. The following commands are implemented:
 * <ul>
 *     <li>connect - to connect to a webSocket</li>
 *     <li>subscribe - to subscribe to a topic</li>
 *     <li>unsubscribe - to unsubscribe from a topic</li>
 *     <li>send - to send a payload to a destination</li>
 * </ul>
 */
public class StompMessageHelper {
    private final static Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    private final static String EMPTY_LINE = "";
    private final static String DELIMITER = ":";
    private final static String END = "\u0000";
    private final static String NEW_LINE = "\n";
    private final static String DESTINATION = "destination";
    private final static String ID = "id";
    private final static String ACK = "ack";
    private final static String RECEIPT = "receipt";

    /**
     * It parses the current STOMP message and returns a {@link Message}.
     * @return the wrapped STOMP message as {@link Message}
     */
    public static Message parseStompMessage(String rawStompMessage) {
        String[] splitMessage = rawStompMessage.split(NEW_LINE);

        if (splitMessage.length == 0)
            throw new IllegalStateException("Did not received any message");

        String command = splitMessage[0];
        StompHeaders stompHeaders = new StompHeaders();
        String body = "";

        int cursor = 1;
        for (int i = cursor; i < splitMessage.length; i++) {
            // empty line
            if (splitMessage[i].equals(EMPTY_LINE)) {
                cursor = i;
                break;
            }
            else {
                String[] header = splitMessage[i].split(DELIMITER);
                stompHeaders.add(header[0], header[1]);
            }
        }

        for (int i = cursor; i < splitMessage.length; i++)
            body += splitMessage[i];

        if (body.isEmpty())
            return new Message(StompCommand.valueOf(command), stompHeaders);
        else
            return new Message(StompCommand.valueOf(command), stompHeaders, body.replace(END, ""));
    }

    public static String buildSubscribeMessage(String destination, String id) {
        String headers = buildHeader(StompCommand.SUBSCRIBE.name());
        headers += buildHeader(DESTINATION, destination);
        headers += buildHeader(ID, id);
        headers += buildHeader(ACK, "auto");
        headers += buildHeader(RECEIPT, "receipt_" + destination);

        return headers + NEW_LINE + END;
    }

    public static String buildUnsubscribeMessage(String subscriptionId) {
        String headers = buildHeader(StompCommand.UNSUBSCRIBE.name());
        headers += buildHeader(ID, subscriptionId);

        return headers + NEW_LINE + END;
    }

    public static String buildConnectMessage() {
        String headers = buildHeader(StompCommand.CONNECT.name());
        headers += buildHeader("accept-version", "1.0,1.1,2.0");
        headers += buildHeader("host", "stomp.github.org");
        headers += buildHeader("heart-beat", "0,0");

        return headers + NEW_LINE + END;
    }

    public static <T> String buildSendMessage(String destination, T payload) {
        String body = payload != null ? gson.toJson(payload) : "";

        String headers = buildHeader(StompCommand.SEND.name());
        headers += buildHeader(DESTINATION, destination);

        return headers + NEW_LINE + body + NEW_LINE + END;
    }

    private static String buildHeader(String key, String value) {
        if (value != null)
            return key + ':' + value + NEW_LINE;
        else
        	return key + NEW_LINE;
    }

    private static String buildHeader(String key) {
        return buildHeader(key, null);
    }
}