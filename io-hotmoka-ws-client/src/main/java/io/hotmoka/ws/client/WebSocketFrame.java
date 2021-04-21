package io.hotmoka.ws.client;

public class WebSocketFrame {
	private final com.neovisionaries.ws.client.WebSocketFrame parent;

	private WebSocketFrame(com.neovisionaries.ws.client.WebSocketFrame parent) {
		this.parent = parent;
	}

	public static WebSocketFrame fromNative(com.neovisionaries.ws.client.WebSocketFrame parent) {
		if (parent == null)
			return null;
		else
			return new WebSocketFrame(parent);
	}

	public com.neovisionaries.ws.client.WebSocketFrame toNative() {
		return parent;
	}
}