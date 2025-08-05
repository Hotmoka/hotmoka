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

import static java.math.BigInteger.ONE;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.AbstractNodeDecorator;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.JarsNode;
import io.hotmoka.helpers.api.MisbehavingNodeException;
import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * A decorator of a node, that installs some jars in the node.
 */
public class JarsNodeImpl extends AbstractNodeDecorator<Node> implements JarsNode {

	/**
	 * The references to the jars installed in the node.
	 */
	private final TransactionReference[] jars;

	/**
	 * Installs the given set of jars in the parent node and creates an object that provides
	 * access to a set of such installed jars. The given account pays for the transactions.
	 * 
	 * @param parent the node to decorate
	 * @param payer the account that pays for the transactions that initialize the new accounts
	 * @param privateKeyOfPayer the private key of the account that pays for the transactions.
	 *                          It will be used to sign requests for installing the jars;
	 *                          the account must have enough coins for those transactions
	 * @param jars the jars to install in the node
	 * @throws TransactionRejectedException if some transaction is rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction throws an exception
	 * @throws IOException if some jar file cannot be accessed
	 * @throws SignatureException if a signature with {@code privateKeyOfPayer} failed
	 * @throws InvalidKeyException if {@code privateKeyOfPayer} is invalid
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws UnknownReferenceException if {@code payer} cannot be found in {@code parent}
	 * @throws NoSuchAlgorithmException if the signature algorithm of {@code payer} is not available
	 * @throws UninitializedNodeException if the node is not initialized yet
	 * @throws UnsupportedVerificationVersionException if {@code parent} uses a verification version that is not available
	 * @throws ClosedNodeException if the node is already closed
	 * @throws MisbehavingNodeException if the node is performing in a buggy way
	 * @throws UnexpectedCodeException if the node contains unexpected runtime classes installed in store
     */
	public JarsNodeImpl(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, Path... jars) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, TimeoutException, InterruptedException, UnknownReferenceException, NoSuchAlgorithmException, UninitializedNodeException, UnsupportedVerificationVersionException, ClosedNodeException, MisbehavingNodeException, UnexpectedCodeException {
		super(parent);

		TransactionReference takamakaCode = getTakamakaCode();
		var signature = SignatureHelpers.of(this).signatureAlgorithmFor(payer);
		Signer<SignedTransactionRequest<?>> signerOnBehalfOfPayer = signature.getSigner(privateKeyOfPayer, SignedTransactionRequest::toByteArrayWithoutSignature);

		// we get the nonce of the payer
		BigInteger nonce = NonceHelpers.of(parent).getNonceOf(payer);

		// we get the chainId of the parent
		String chainId = parent.getConfig().getChainId();
		var gasHelper = GasHelpers.of(this);
		var jarSuppliers = new JarFuture[jars.length];
		int pos = 0;

		for (Path jar: jars) {
			byte[] bytes = Files.readAllBytes(jar);
			jarSuppliers[pos++] = postJarStoreTransaction(TransactionRequests.jarStore
				(signerOnBehalfOfPayer, payer, nonce, chainId, BigInteger.valueOf(200000 + bytes.length * 400L), gasHelper.getSafeGasPrice(), takamakaCode, bytes, takamakaCode));
			nonce = nonce.add(ONE);
		}

		// we wait for them
		pos = 0;
		this.jars = new TransactionReference[jarSuppliers.length];
		for (var jarSupplier: jarSuppliers)
			this.jars[pos++] = jarSupplier.get();
	}

	@Override
	public TransactionReference jar(int i) throws ClosedNodeException, NoSuchElementException {
		ensureNotClosed();
			
		if (i < 0 || i >= jars.length)
			throw new NoSuchElementException();

		return jars[i];
	}
}