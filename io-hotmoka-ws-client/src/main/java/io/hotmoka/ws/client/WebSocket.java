package io.hotmoka.ws.client;

public class WebSocket {
	private final com.neovisionaries.ws.client.WebSocket parent;

	private WebSocket(com.neovisionaries.ws.client.WebSocket parent) {
		this.parent = parent;
	}

	public static WebSocket fromNative(com.neovisionaries.ws.client.WebSocket parent) {
		if (parent == null)
			return null;
		else
			return new WebSocket(parent);
	}

	public com.neovisionaries.ws.client.WebSocket toNative() {
		return parent;
	}

	public void sendText(String message) {
		parent.sendText(message);
	}

	public void disconnect(int closeCode) {
		parent.disconnect(closeCode);
	}

	public WebSocket addHeader(String name, String value) {
		return fromNative(parent.addHeader(name, value));
	}

	public WebSocket addListener(WebSocketAdapter adapter) {
		return fromNative(parent.addListener(adapter.toNative()));
	}

	public WebSocket connect() throws WebSocketException {
		try {
			return fromNative(parent.connect());
		}
		catch (com.neovisionaries.ws.client.WebSocketException e) {
			throw WebSocketException.fromNative(e);
		}
	}
}