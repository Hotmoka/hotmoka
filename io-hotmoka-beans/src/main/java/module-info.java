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
 * This module implements the beans exchanged among Hotmoka nodes.
 */
module io.hotmoka.beans {
	exports io.hotmoka.beans.marshalling;
	exports io.hotmoka.beans.requests;
	exports io.hotmoka.beans.responses;
	exports io.hotmoka.beans.updates;
	exports io.hotmoka.beans;

	// beans must be accessible, encoded and decoded by reflection through Gson
	opens io.hotmoka.beans.internal.gson to com.google.gson;

	requires transitive io.hotmoka.beans.api;
	requires transitive io.hotmoka.marshalling;
	requires io.hotmoka.constants;
	requires io.hotmoka.crypto;
	requires io.hotmoka.annotations;
	requires io.hotmoka.websockets.beans;
	requires com.google.gson;
}