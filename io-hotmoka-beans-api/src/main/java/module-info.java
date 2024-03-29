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
 * This module defines the API of the beans exchanged among Hotmoka nodes.
 */
module io.hotmoka.beans.api {
	exports io.hotmoka.beans.api.nodes;
	exports io.hotmoka.beans.api.requests;
	exports io.hotmoka.beans.api.responses;
	exports io.hotmoka.beans.api.signatures;
	exports io.hotmoka.beans.api.transactions;
	exports io.hotmoka.beans.api.types;
	exports io.hotmoka.beans.api.updates;
	exports io.hotmoka.beans.api.values;

	requires transitive io.hotmoka.marshalling.api;
	requires io.hotmoka.annotations;
}