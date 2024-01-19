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

package io.hotmoka.beans.api.requests;

import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;

/**
 * A request for a transaction that installs a jar in a node.
 * 
 * @param <R> the type of the response expected for this request
 */
@Immutable
public interface JarStoreTransactionRequest<R extends JarStoreTransactionResponse> extends TransactionRequest<R> {
	
	/**
	 * Yields the bytes of the jar to install.
	 * 
	 * @return the bytes of the jar to install
	 */
	byte[] getJar();

	/**
	 * Yields the length, in bytes, of the jar to install.
	 * 
	 * @return the length
	 */
	int getJarLength();

	/**
	 * Yields the dependencies of the jar to install.
	 * 
	 * @return the dependencies, as an ordered stream. The order should be the same
	 *         as in the arguments provided to the constructor of the request
	 */
	Stream<TransactionReference> getDependencies();

	/**
	 * Yields the number of dependencies.
	 * 
	 * @return the number of dependencies
	 */
	int getNumberOfDependencies();
}