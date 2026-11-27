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

package io.hotmoka.node.api.requests;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;

/**
 * A request for a transaction that installs a jar in a yet non-initialized node.
 * Verification for the jar is more permissive than for the normal installation of jars.
 * This request might also be executed after the node has been initialized, but only
 * under specific conditions that are specified by the consensus rules of the node.
 * For instance, it might be executed to replace the Takamaka code jar of the node
 * if a given block height has been reached.
 */
@Immutable
public interface JarStoreInitialTransactionRequest extends InitialTransactionRequest<JarStoreInitialTransactionResponse>, GenericJarStoreTransactionRequest<JarStoreInitialTransactionResponse> {

	/**
	 * Determines if the installed jar must be set as Takamaka code.
	 * 
	 * @return true if and only if the installed jar must be set as Takamaka code
	 */
	boolean jarShouldBeSetAsTakamakaCode();
}