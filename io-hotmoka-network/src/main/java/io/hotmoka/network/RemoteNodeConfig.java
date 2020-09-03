package io.hotmoka.network;

/**
 * The configuration of a node that forwards all its calls to a remote network service.
 */
public class RemoteNodeConfig {

	/**
	 * The URL of the remote service. This defaults to http://localhost:8080
	 */
	public final String url;

	/**
	 * True if and only if web sockets should be used for the connection.
	 * This defaults to false.
	 */
	public final boolean webSockets;

	/**
     * Builds the configuration from a builder.
     * 
     * @param builder the builder
     */
    private RemoteNodeConfig(Builder builder) {
        this.url = builder.url;
        this.webSockets = builder.webSockets;
    }

    /**
     * The builder of a configuration of a remote node.
     */
    public static class Builder {
    	private String url = "http://localhost:8080";

    	private boolean webSockets;

    	/**
         * Specifies if the URL of the remote service.
         * The default is http://localhost:8080.
         * If you select the use of websockets, then
         * this should be set to something like
         * ws://localhost:8080.
         * 
         * @param url the url
         * @return this same builder
         */
        public Builder setURL(String url) {
        	this.url = url;
            return this;
        }

        /**
         * Sets the use of websockets.
         * 
         * @param webSockets true if and only if websockets should be used
         *                   instead of http connections. This defaults to false
         * @return thie same builder
         */
        public Builder setWebSockets(boolean webSockets) {
        	this.webSockets = webSockets;
        	return this;
        }

        /**
         * Builds the configuration from this builder.
         * 
         * @return the configuration
         */
        public RemoteNodeConfig build() {
            return new RemoteNodeConfig(this);
        }
    }
}