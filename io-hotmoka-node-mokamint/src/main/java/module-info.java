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
 * This module implements Hotmoka nodes running over the Mokamint engine.
 * They can be used to build an actual blockchain network based on proof of space.
 */
module io.hotmoka.node.mokamint {
	exports io.hotmoka.node.mokamint;

	requires transitive io.hotmoka.node.mokamint.api;
	requires transitive io.hotmoka.node.local.api;
	requires io.hotmoka.node;
	requires io.hotmoka.node.local;
	requires io.hotmoka.annotations;
	requires io.hotmoka.exceptions;
	requires io.hotmoka.xodus;
	requires io.hotmoka.crypto;
	requires transitive io.mokamint.node.local.api;
	requires io.mokamint.node.local;
	requires io.mokamint.node;
	requires io.mokamint.application;
	requires toml4j;
	requires java.logging;
}