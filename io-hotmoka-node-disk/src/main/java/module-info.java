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
 * This module implements Hotmoka nodes that store their data on disk.
 * They do not form a blockchain network, since there is no networking nor mining.
 */
module io.hotmoka.node.disk {
	exports io.hotmoka.node.disk;

	requires transitive io.hotmoka.node.disk.api;
	requires transitive io.hotmoka.helpers.api;
	requires io.hotmoka.node;
	requires io.hotmoka.annotations;
	requires io.hotmoka.node.local;
	requires transitive io.hotmoka.helpers;
	requires io.hotmoka.constants;
	requires toml4j;
	requires java.logging;
}