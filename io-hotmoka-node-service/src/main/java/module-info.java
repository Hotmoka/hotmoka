/*
Copyright 2024 Fausto Spoto

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

/**
 * This module implements a network service that publishes a Hotmoka node.
 */
module io.hotmoka.node.service {
	exports io.hotmoka.node.service;
	// needed to allow the endpoints to be created by reflection although they are not exported
	opens io.hotmoka.node.service.internal to org.glassfish.tyrus.core;

    requires transitive io.hotmoka.node.service.api;
    requires transitive io.hotmoka.node.api;
	requires io.hotmoka.closeables.api;
	requires io.hotmoka.node.messages;
	requires io.hotmoka.annotations;
	requires io.hotmoka.websockets.server;
	requires io.hotmoka.websockets.beans;
	requires static org.glassfish.tyrus.core;
    requires java.logging;
    requires transitive toml4j;
}