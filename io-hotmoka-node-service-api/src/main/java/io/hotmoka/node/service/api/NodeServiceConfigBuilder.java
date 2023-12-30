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

/**
 * The builder of a configuration object.
 */
public interface NodeServiceConfigBuilder {

    /**
     * Sets the HTTP port of the network service.
     * It defaults to 8080.
     * 
     * @param port the port
     * @return this same builder
     */
	NodeServiceConfigBuilder setPort(int port);

    /**
     * Builds the configuration from this builder.
     * 
     * @return the configuration
     */
	NodeServiceConfig build();
}