/*
Copyright 2025 Fausto Spoto

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
 * This module defines the API of the command-line tool for running basic commands against a Hotmoka node.
 */
module io.hotmoka.moka.api {
	exports io.hotmoka.moka.api;
	exports io.hotmoka.moka.api.accounts;
	exports io.hotmoka.moka.api.jars;
	exports io.hotmoka.moka.api.keys;
	exports io.hotmoka.moka.api.nodes;
	exports io.hotmoka.moka.api.nodes.config;
	exports io.hotmoka.moka.api.nodes.disk;
	exports io.hotmoka.moka.api.nodes.manifest;
	exports io.hotmoka.moka.api.nodes.mokamint;
	exports io.hotmoka.moka.api.nodes.takamaka;
	exports io.hotmoka.moka.api.nodes.tendermint;
	exports io.hotmoka.moka.api.nodes.tendermint.validators;
	exports io.hotmoka.moka.api.objects;

	requires transitive io.hotmoka.node.api;
	requires io.hotmoka.annotations;
}