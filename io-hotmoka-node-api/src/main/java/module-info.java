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

/**
 * This module defines the API of the shared code of all Hotmoka nodes.
 */
module io.hotmoka.node.api {
	exports io.hotmoka.node.api;
	exports io.hotmoka.node.api.nodes;
	exports io.hotmoka.node.api.requests;
	exports io.hotmoka.node.api.responses;
	exports io.hotmoka.node.api.signatures;
	exports io.hotmoka.node.api.transactions;
	exports io.hotmoka.node.api.types;
	exports io.hotmoka.node.api.updates;
	exports io.hotmoka.node.api.values;

	requires transitive io.hotmoka.marshalling.api;
	requires transitive io.hotmoka.crypto.api;
	requires transitive io.hotmoka.instrumentation.api;
	requires transitive io.hotmoka.verification.api;
	requires io.hotmoka.annotations;
	requires io.hotmoka.closeables.api;
}