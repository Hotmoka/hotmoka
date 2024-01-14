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

package io.hotmoka.helpers.internal;

import java.math.BigInteger;
import java.util.NoSuchElementException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.node.api.Node;

/**
 * Implementation of an object that helps with nonce operations.
 */
public class NonceHelperImpl implements NonceHelper {
	private final Node node;
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * Creates an object that helps with nonce operations.
	 * 
	 * @param node the node whose accounts are considered
	 */
	public NonceHelperImpl(Node node) {
		this.node = node;
	}

	@Override
	public BigInteger getNonceOf(StorageReference account) throws TransactionRejectedException, NoSuchElementException, TransactionException, CodeExecutionException {
		// we ask the account: 100,000 units of gas should be enough to run the method
		return ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(account, _100_000, node.getClassTag(account).jar, CodeSignature.NONCE, account))).getValue();
	}
}