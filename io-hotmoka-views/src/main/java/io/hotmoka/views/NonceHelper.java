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

package io.hotmoka.views;

import java.math.BigInteger;
import java.util.NoSuchElementException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;

/**
 * An object that helps with nonce operations.
 */
public class NonceHelper {
	private final Node node;
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * Creates an object that helps with nonce operations.
	 * 
	 * @param node the node whose accounts are considered
	 */
	public NonceHelper(Node node) {
		this.node = node;
	}

	/**
	 * Yields the nonce of an account.
	 * 
	 * @param account the account
	 * @return the nonce of {@code account}
	 */
	public BigInteger getNonceOf(StorageReference account) throws TransactionRejectedException, NoSuchElementException, TransactionException, CodeExecutionException {
		// we ask the account: 100,000 units of gas should be enough to run the method
		return ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(account, _100_000, node.getClassTag(account).jar, CodeSignature.NONCE, account))).value;
	}
}