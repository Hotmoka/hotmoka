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

module io.hotmoka.beans {
	exports io.hotmoka.beans.marshalling;
	exports io.hotmoka.beans.references;
	exports io.hotmoka.beans.requests;
	exports io.hotmoka.beans.responses;
	exports io.hotmoka.beans.signatures;
	exports io.hotmoka.beans.types;
	exports io.hotmoka.beans.updates;
	exports io.hotmoka.beans.values;
	exports io.hotmoka.beans.nodes;
	exports io.hotmoka.beans;
	requires transitive io.hotmoka.marshalling;
	requires io.hotmoka.constants;
	requires transitive io.hotmoka.crypto.api;
	requires io.hotmoka.annotations;
}