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
 * This module implements the network messages exchanged between a node service and remote.
 */
module io.hotmoka.node.messages {
	exports io.hotmoka.node.messages;
	// beans must be accessible, encoded and decoded by reflection through Gson
	opens io.hotmoka.node.messages.internal to com.google.gson;
	opens io.hotmoka.node.messages.internal.gson to com.google.gson;

	requires transitive io.hotmoka.node.messages.api;
	requires io.hotmoka.node.api;
	requires io.hotmoka.node;
	requires io.hotmoka.websockets.beans;
	requires com.google.gson;
}