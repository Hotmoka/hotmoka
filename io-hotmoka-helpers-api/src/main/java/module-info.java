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
 * This module defines the API of the helper classes that provide
 * frequently used sets of Hotmoka transactions.
 */
module io.hotmoka.helpers.api {
	exports io.hotmoka.helpers.api;

	requires io.hotmoka.annotations;
	requires transitive io.hotmoka.crypto.api;
	requires transitive io.hotmoka.node.api;
	requires transitive io.hotmoka.verification.api;
	requires transitive io.hotmoka.whitelisting.api;
}