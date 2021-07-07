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

package io.hotmoka.service;

/**
 * The configuration of a network service supported by a Hotmoka node.
 */
public class NodeServiceConfig {

	/**
	 * The HTTP port of the server.
	 */
	public final int port;

	/**
	 * True if and only if the Spring banner must be shown when the service starts.
	 */
	public final boolean showSpringBanner;

    /**
     * Builds the configuration from a builder.
     * 
     * @param builder the builder
     */
    private NodeServiceConfig(Builder builder) {
        this.port = builder.port;
        this.showSpringBanner = builder.showSpringBanner;
    }

    /**
     * The builder of a configuration of a network service.
     */
    public static class Builder {
        private int port = 8080;
        private boolean showSpringBanner = false;

        /**
         * Specifies if the network service, at its start, prints
         * the Spring banner on the standard output, or not.
         * The default is false.
         * 
         * @param showSpringBanner true if and only if the banner must be print
         * @return this same builder
         */
        public Builder setSpringBannerModeOn(boolean showSpringBanner) {
            this.showSpringBanner = showSpringBanner;
            return this;
        }

        /**
         * Sets the HTTP port of the network service.
         * It defaults to 8080.
         * 
         * @param port the port
         * @return this same builder
         */
        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        /**
         * Builds the configuration from this builder.
         * 
         * @return the configuration
         */
        public NodeServiceConfig build() {
            return new NodeServiceConfig(this);
        }
    }
}