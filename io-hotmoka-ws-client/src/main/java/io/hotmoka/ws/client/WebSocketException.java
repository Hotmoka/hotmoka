package io.hotmoka.ws.client;

public class WebSocketException extends Exception {
	private static final long serialVersionUID = 6842969946302682805L;

	private final com.neovisionaries.ws.client.WebSocketException parent;

	private WebSocketException(com.neovisionaries.ws.client.WebSocketException parent) {
		this.parent = parent;
	}

	public static WebSocketException fromNative(com.neovisionaries.ws.client.WebSocketException parent) {
		if (parent == null)
			return null;
		else
			return new WebSocketException(parent);
	}

	@Override
	public String getMessage() {
		return parent.getMessage();
    }

	@Override
	public Throwable getCause() {
		return parent.getCause();
	}
}