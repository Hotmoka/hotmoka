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

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.api.SignatureAlgorithm;

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
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be loaded
	 * @throws NoSuchAlgorithmException if the sender uses an unknown signature algorithm
	 */
	SignatureAlgorithm signatureAlgorithmFor(StorageReference account) throws NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException;
}