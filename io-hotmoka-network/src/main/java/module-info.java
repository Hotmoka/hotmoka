/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

module io.hotmoka.network {
	exports io.hotmoka.network.requests;
	exports io.hotmoka.network.values;
	exports io.hotmoka.network.updates;
	exports io.hotmoka.network.signatures;
	exports io.hotmoka.network.responses;
	exports io.hotmoka.network.errors;
	exports io.hotmoka.network.nodes;
	exports io.hotmoka.network;

    // Gson needs superpowers
    opens io.hotmoka.network.errors to com.google.gson;
    opens io.hotmoka.network.requests to com.google.gson;
    opens io.hotmoka.network.responses to com.google.gson;
    opens io.hotmoka.network.signatures to com.google.gson;
    opens io.hotmoka.network.updates to com.google.gson;
    opens io.hotmoka.network.values to com.google.gson;
    opens io.hotmoka.network.nodes to com.google.gson;

	requires transitive io.hotmoka.beans;
}