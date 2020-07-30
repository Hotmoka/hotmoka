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
     * Builds the configuration from a builder.
     * 
     * @param builder the builder
     */
    private RemoteNodeConfig(Builder builder) {
        this.url = builder.url;
    }

    /**
     * The builder of a configuration of a remote node.
     */
    public static class Builder {
    	private String url = "http://localhost:8080";

    	/**
         * Specifies if the URL of the remote service.
         * The default is http://localhost:8080.
         * 
         * @param url the url
         * @return this same builder
         */
        public Builder setURL(String url) {
        	this.url = url;
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