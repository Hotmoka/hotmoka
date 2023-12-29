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

import io.hotmoka.annotations.Immutable;

/**
 * The configuration of a node that forwards all its calls to a remote network service.
 */
@Immutable
public interface RemoteNodeConfig {

	/**
     * Yields the URL of the remote service, without the protocol. This defaults
     * to {@code localhost:8080}.
     * 
     * @return the URL
     */
    String getURL();

    /**
     * Determines if web sockets should be used for the connection. This defaults to false.
     * 
     * @return true if and only if web sockets should be used for the connection
     */
    boolean usesWebSockets();

    /**
	 * Yields a TOML representation of this configuration.
	 * 
	 * @return the TOML representation, as a string
	 */
	String toToml();

	@Override
	boolean equals(Object other);

	@Override
	String toString();

	/**
	 * Yields a builder initialized with the information in this object.
	 * 
	 * @return the builder
	 */
	RemoteNodeConfigBuilder toBuilder();
}