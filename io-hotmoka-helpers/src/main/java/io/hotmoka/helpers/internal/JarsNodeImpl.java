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
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A decorator of a node, that installs some jars in the node.
 */
public class JarsNodeImpl extends AbstractNodeDecorator<Node> implements JarsNode {

	/**
	 * The references to the jars installed in the node.
	 */
	private final TransactionReference[] jars;

	/**
	 * Installs the given set of jars in the parent node and
	 * creates a view that provides access to a set of previously installed jars.
	 * The given account pays for the transactions.
	 * 
	 * @param parent the node to decorate
	 * @param payer the account that pays for the transactions that initialize the new accounts
	 * @param privateKeyOfPayer the private key of the account that pays for the transactions.
	 *                          It will be used to sign requests for installing the jars;
	 *                          the account must have enough coins for those transactions
	 * @param jars the jars to install in the node
	 * @throws TransactionException if some transaction that installs the jars fails
	 * @throws CodeExecutionException if some transaction that installs the jars throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 * @throws UnknownReferenceException if {@code payer} cannot be found in {@code parent}
     */
	public JarsNodeImpl(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, Path... jars) throws TransactionRejectedException, TransactionException, IOException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		super(parent);

		TransactionReference takamakaCode = getTakamakaCode();
		var signature = SignatureHelpers.of(this).signatureAlgorithmFor(payer);
		Signer<SignedTransactionRequest<?>> signerOnBehalfOfPayer = signature.getSigner(privateKeyOfPayer, SignedTransactionRequest::toByteArrayWithoutSignature);

		// we get the nonce of the payer
		BigInteger nonce = NonceHelpers.of(parent).getNonceOf(payer, takamakaCode);

		// we get the chainId of the parent
		String chainId = parent.getConfig().getChainId();

		var gasHelper = GasHelpers.of(this);
		var jarSuppliers = new JarFuture[jars.length];
		int pos = 0;
		for (Path jar: jars) {
			byte[] bytes = Files.readAllBytes(jar);
			jarSuppliers[pos] = postJarStoreTransaction(TransactionRequests.jarStore
				(signerOnBehalfOfPayer, payer, nonce, chainId, BigInteger.valueOf(10000 + bytes.length * 200L), gasHelper.getSafeGasPrice(), takamakaCode, bytes, takamakaCode));
			nonce = nonce.add(ONE);
			pos++;
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