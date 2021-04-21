package io.hotmoka.ws.client;

import java.io.IOException;

public class WebSocketFactory {
	private final com.neovisionaries.ws.client.WebSocketFactory parent;

	private WebSocketFactory(com.neovisionaries.ws.client.WebSocketFactory parent) {
		this.parent = parent;
	}

	public WebSocketFactory() {
		this.parent = new com.neovisionaries.ws.client.WebSocketFactory();
	}

	public static WebSocketFactory fromNative(com.neovisionaries.ws.client.WebSocketFactory parent) {
		if (parent == null)
			return null;
		else
			return new WebSocketFactory(parent);
	}

	public com.neovisionaries.ws.client.WebSocketFactory toNative() {
		return parent;
	}

	public WebSocketFactory setConnectionTimeout(int i) {
		return WebSocketFactory.fromNative(parent.setConnectionTimeout(i));
	}

	public WebSocket createSocket(String url) throws IOException {
		return WebSocket.fromNative(parent.createSocket(url));
	}
}