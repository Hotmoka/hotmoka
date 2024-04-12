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

/**
 * This module implements a network service that publishes a Hotmoka node.
 */
module io.hotmoka.node.service {
	exports io.hotmoka.node.service;
	// needed to allow the endpoints to be created by reflection although they are not exported
	opens io.hotmoka.node.service.internal to org.glassfish.tyrus.core, spring.core;

	// Spring needs superpowers
	exports io.hotmoka.node.service.internal to spring.beans, spring.context;

    requires transitive io.hotmoka.node.service.api;
    requires transitive io.hotmoka.node;
	requires transitive io.hotmoka.beans;
	requires transitive io.hotmoka.network;
	requires io.hotmoka.node.messages;
	requires io.hotmoka.annotations;	
	requires io.hotmoka.websockets.server;
	requires io.hotmoka.websockets.beans;
	requires transitive jakarta.websocket.client;
	requires org.glassfish.tyrus.core;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.beans;
    requires spring.web;
    requires spring.context;
    requires spring.boot.starter.websocket;
    requires spring.websocket;
    requires transitive spring.messaging;
    requires com.google.gson;
    requires java.instrument;
    requires java.logging;
    requires transitive toml4j;
    requires org.apache.tomcat.embed.websocket;

    // these make it possible to compile under Eclipse...
    requires static spring.core;
    requires static org.apache.tomcat.embed.core;
}