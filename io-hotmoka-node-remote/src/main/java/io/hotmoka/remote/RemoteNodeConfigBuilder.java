/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.remote;

/**
 * The builder of a configuration object.
 */
public interface RemoteNodeConfigBuilder {

	/**
	 * Specifies the URL of the remote service, without the protocol.
	 * The default is {@code localhost:8080}.
	 *
	 * @param url the url
	 * @return this same builder
	 */
	RemoteNodeConfigBuilder setURL(String url);

	/**
	 * Sets the use of websockets.
	 *
	 * @param usesWebSockets true if and only if websockets should be used
	 *                       instead of http connections. This defaults to false
	 * @return this same builder
	 */
	RemoteNodeConfigBuilder usesWebSockets(boolean usesWebSockets);

	/**
     * Builds the configuration from this builder.
     *
     * @return the configuration
     */
	RemoteNodeConfig build();
}