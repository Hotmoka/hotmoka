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
 * This module implements the shared code of all Hotmoka nodes.
 */
module io.hotmoka.node {
	exports io.hotmoka.node;
	// beans must be accessible, encoded and decoded by reflection through Gson
	opens io.hotmoka.node.internal.gson to com.google.gson;

	requires transitive io.hotmoka.node.api;
	requires io.hotmoka.crypto;
	requires io.hotmoka.annotations;
	requires transitive io.hotmoka.marshalling;
	requires io.hotmoka.exceptions;
	requires io.hotmoka.websockets.beans;
	requires io.takamaka.code.constants;
	requires toml4j;
	requires static com.google.gson;
	requires java.logging;

	// this makes sun.misc.Unsafe accessible, so that Gson can instantiate
	// classes without the no-args constructor
	requires jdk.unsupported;
}