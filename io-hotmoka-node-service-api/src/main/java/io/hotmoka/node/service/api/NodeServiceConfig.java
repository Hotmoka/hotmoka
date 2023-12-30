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

package io.hotmoka.node.service.api;

import io.hotmoka.annotations.Immutable;

/**
 * The configuration of a network service that publishes a Hotmoka node.
 */
@Immutable
public interface NodeServiceConfig {

	/**
     * Yields the HTTP port of the server.
     * 
     * @return the HTTP port
     */
    int getPort();

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
	 * Yields a builder initialized with the information in this configuration.
	 * 
	 * @return the builder
	 */
	NodeServiceConfigBuilder toBuilder();
}