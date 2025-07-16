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
import java.util.concurrent.TimeoutException;

import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.helpers.UnexpectedVoidMethodException;
import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.values.StorageReference;

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
	public BigInteger getNonceOf(StorageReference account) throws TransactionRejectedException, TransactionException, ClosedNodeException, UnexpectedCodeException, TimeoutException, InterruptedException, CodeExecutionException, UnknownReferenceException {
		var classpath = node.getClassTag(account).getJar();
		return node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(account, _100_000, classpath, MethodSignatures.NONCE, account))
				.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.NONCE))
				.asReturnedBigInteger(MethodSignatures.NONCE, UnexpectedValueException::new);
	}
}