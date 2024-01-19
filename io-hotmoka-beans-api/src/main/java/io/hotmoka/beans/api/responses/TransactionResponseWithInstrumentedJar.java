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

package io.hotmoka.beans.api.responses;

import java.util.stream.Stream;

import io.hotmoka.beans.api.transactions.TransactionReference;

/**
 * A response for a transaction that successfully installed a jar in the blockchain.
 */
public interface TransactionResponseWithInstrumentedJar extends TransactionResponse {

	/**
	 * Yields the bytes of the installed jar.
	 * 
	 * @return the bytes of the installed jar
	 */
	byte[] getInstrumentedJar();

	/**
	 * Yields the size of the instrumented jar, in bytes.
	 * 
	 * @return the size
	 */
	int getInstrumentedJarLength();

	/**
	 * Yields the dependencies of the jar, previously installed in blockchain.
	 * 
	 * @return the dependencies
	 */
	Stream<TransactionReference> getDependencies();
	
	/**
	 * Yields the version of the verification module that was used to verify the jar.
	 * 
	 * @return the version
	 */
	long getVerificationVersion();
}