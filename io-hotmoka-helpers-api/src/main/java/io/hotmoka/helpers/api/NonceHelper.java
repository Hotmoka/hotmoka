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

import java.math.BigInteger;
import java.util.NoSuchElementException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * An object that helps with nonce operations.
 */
@ThreadSafe
public interface NonceHelper {

	/**
	 * Yields the nonce of an account.
	 * 
	 * @param account the account
	 * @return the nonce of {@code account}
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws NoSuchElementException if the class of the account cannot be determined
	 */
	BigInteger getNonceOf(StorageReference account) throws TransactionRejectedException, NoSuchElementException, TransactionException, CodeExecutionException;
}