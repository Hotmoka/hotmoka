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
 * This module implements a local Hotmoka node.
 */
module io.hotmoka.node.local {
	exports io.hotmoka.node.local;
	
	// classes inside this package will be used only at run time,
	// by instrumented Takamaka code: we do not want to export it:
	// we make them visible at compile time only instead
	opens io.hotmoka.node.local.internal.runtime;

	requires transitive io.hotmoka.node.local.api;
	requires transitive io.hotmoka.node;
	requires transitive io.hotmoka.closeables.api;
	requires io.hotmoka.crypto;
	requires io.hotmoka.patricia;
	requires io.hotmoka.annotations;
	requires io.hotmoka.xodus;
	requires io.hotmoka.instrumentation;
	requires io.hotmoka.whitelisting;
	requires transitive io.hotmoka.whitelisting.api;
	requires io.hotmoka.exceptions;
	requires io.hotmoka.closeables;
	requires io.takamaka.code.constants;
	requires toml4j;
	requires java.logging;
	requires io.hotmoka.node.api;
}