package io.hotmoka.ws.client;

import java.util.List;
import java.util.Map;

public class WebSocketAdapter {

	public com.neovisionaries.ws.client.WebSocketAdapter toNative() {
		return new com.neovisionaries.ws.client.WebSocketAdapter() {
			
			@Override
			public void onConnected(com.neovisionaries.ws.client.WebSocket websocket, Map<String, List<String>> headers) throws Exception {
				WebSocketAdapter.this.onConnected(WebSocket.fromNative(websocket), headers);
			}

			@Override
			public void onDisconnected(com.neovisionaries.ws.client.WebSocket websocket, com.neovisionaries.ws.client.WebSocketFrame serverCloseFrame, com.neovisionaries.ws.client.WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
				WebSocketAdapter.this.onDisconnected(WebSocket.fromNative(websocket), WebSocketFrame.fromNative(serverCloseFrame), WebSocketFrame.fromNative(clientCloseFrame), closedByServer);
			}

			@Override
			public void onTextMessage(com.neovisionaries.ws.client.WebSocket websocket, String txtMessage) {
				WebSocketAdapter.this.onTextMessage(WebSocket.fromNative(websocket), txtMessage);
			}

			@Override
			public void onError(com.neovisionaries.ws.client.WebSocket websocket, com.neovisionaries.ws.client.WebSocketException cause) throws Exception {
				WebSocketAdapter.this.onError(WebSocket.fromNative(websocket), WebSocketException.fromNative(cause));
			}
		};
	}

	public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {}

	public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {}

	public void onTextMessage(WebSocket websocket, String txtMessage) {}

	public void onError(WebSocket websocket, WebSocketException cause) throws Exception {}
}