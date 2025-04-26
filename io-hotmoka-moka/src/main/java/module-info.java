/*
Copyright 2021 Fausto Spoto

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
 * This module implements a command-line tool for running basic commands against a Hotmoka node.
 */
module io.hotmoka.moka {
	exports io.hotmoka.moka;

	// for injecting CLI options
	opens io.hotmoka.moka to info.picocli;
    opens io.hotmoka.moka.internal to info.picocli;
    opens io.hotmoka.moka.internal.converters to info.picocli;
    opens io.hotmoka.moka.internal.accounts to info.picocli;
    opens io.hotmoka.moka.internal.jars to info.picocli, com.google.gson;
    opens io.hotmoka.moka.internal.keys to info.picocli;
    opens io.hotmoka.moka.internal.nodes to info.picocli;
    opens io.hotmoka.moka.internal.json to com.google.gson;
    opens io.hotmoka.moka.internal.nodes.config to info.picocli;
    opens io.hotmoka.moka.internal.nodes.disk to info.picocli;
    opens io.hotmoka.moka.internal.nodes.manifest to info.picocli;
    opens io.hotmoka.moka.internal.nodes.takamaka to info.picocli;

    requires transitive io.hotmoka.moka.api;
    requires io.hotmoka.helpers;
    requires io.hotmoka.node.tendermint;
    requires io.hotmoka.node.mokamint;
	requires io.hotmoka.node.disk;
	requires io.hotmoka.node.local;
	requires io.hotmoka.node.service;
	requires io.hotmoka.node.remote;
	requires io.hotmoka.node.api;
	requires io.hotmoka.instrumentation;
	requires io.hotmoka.whitelisting.api;
	requires io.hotmoka.cli;
	requires transitive io.hotmoka.crypto;
	requires io.hotmoka.exceptions;
	requires io.hotmoka.annotations;
	requires io.takamaka.code.constants;
	requires io.mokamint.node.local;
	requires io.mokamint.plotter;
	requires io.mokamint.miner.local;
	requires io.mokamint.node.service;
	requires jakarta.websocket.client;
	requires io.hotmoka.websockets.client.api;
	requires io.hotmoka.websockets.beans;
	requires info.picocli;
	requires com.google.gson;
	requires java.logging;
}