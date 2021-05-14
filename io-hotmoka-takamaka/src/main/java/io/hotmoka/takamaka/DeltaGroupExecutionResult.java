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

package io.hotmoka.takamaka;

import java.util.stream.Stream;

import io.hotmoka.beans.responses.TransactionResponse;

/**
 * The result of the execution of a delta group.
 */
public interface DeltaGroupExecutionResult {

	/**
	 * Yields the hash of the store that points to its view at the end of the
	 * execution of the requests in the delta group.
	 * 
	 * @return the hash
	 */
	byte[] getHash();

	/**
	 * Yields an ordered stream of the responses of the requests. If some request could
	 * not be executed because is syntactical errors in the request, its position contains {@code null}.
	 * For requests that could be executed but failed in the user code of the smart contracts,
	 * their position will contain a failed transaction response object, but never {@code null}.
	 * 
	 * @return the responses, in the same order as they were scheduled for execution
	 */
	Stream<TransactionResponse> responses();

	/**
	 * Yields the identifier of the execution whose result is this.
	 * 
	 * @return the identifier
	 */
	String getId();
}