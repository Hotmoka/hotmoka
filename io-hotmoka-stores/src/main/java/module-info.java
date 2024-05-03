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
 * This module implements the store where Hotmoka nodes can persist data.
 */
module io.hotmoka.stores {
	exports io.hotmoka.stores;

	requires transitive io.hotmoka.node.api;
	requires io.hotmoka.node;
	requires io.hotmoka.annotations;
	requires io.hotmoka.crypto;
	requires io.hotmoka.patricia;
	requires io.hotmoka.exceptions;
	requires io.hotmoka.verification;
	requires io.hotmoka.instrumentation.api;
	requires transitive io.hotmoka.whitelisting.api;
	requires transitive io.hotmoka.xodus;
	requires java.logging;
}