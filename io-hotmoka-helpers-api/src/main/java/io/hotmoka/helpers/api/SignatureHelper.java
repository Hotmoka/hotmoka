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

package io.hotmoka.helpers.api;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A helper to determine the signature algorithm to use for an externally owned account.
 */
@ThreadSafe
public interface SignatureHelper {

	/**
	 * Yields the signature algorithm to use for signing transactions on behalf of the given account.
	 * 
	 * @param account the account
	 * @return the algorithm
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws InterruptedException if the current thread gets interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws UnknownReferenceException if {@code account} cannot be found in the node
	 * @throws NoSuchAlgorithmException if the signature algorithm used by {@code account} is not available
	 */
	SignatureAlgorithm signatureAlgorithmFor(StorageReference account) throws NodeException, InterruptedException, TimeoutException, UnknownReferenceException, NoSuchAlgorithmException;
}