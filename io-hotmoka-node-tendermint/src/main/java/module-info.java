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
 * This module implements Hotmoka nodes running over the Tendermint engine.
 * They can be used to build an actual blockchain network with validator nodes.
 */
module io.hotmoka.node.tendermint {
	exports io.hotmoka.node.tendermint;
	exports io.hotmoka.node.tendermint.internal.beans to com.google.gson;

	requires transitive io.hotmoka.node.tendermint.api;
	requires io.hotmoka.node.tendermint.abci;
	requires io.hotmoka.stores;
	requires io.hotmoka.annotations;
	requires io.hotmoka.exceptions;
	requires transitive io.hotmoka.helpers;
	requires io.hotmoka.node.local;
	requires io.hotmoka.closeables;
	requires com.google.gson;
	requires com.google.protobuf;
	requires toml4j;
	requires java.logging;
}