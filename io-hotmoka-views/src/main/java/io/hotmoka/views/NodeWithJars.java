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

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.NoSuchElementException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.views.internal.NodeWithJarsImpl;

/**
 * A node that provides access to a set of previously installed jars,
 */
@ThreadSafe
public interface NodeWithJars extends Node {

	/**
	 * Yields the references, in the store of the node, where the {@code it}th jar has been installed.
	 * 
	 * @param i the jar number
	 * @return the reference to the jar, in the store of the node
	 * @throws NoSuchElementException if the {@code i}th installed jar does not exist
	 */
	TransactionReference jar(int i) throws NoSuchElementException;

	/**
	 * Installs the given set of jars in the parent node and
	 * yields a view that provides access to a set of previously installed jars.
	 * The given account pays for the transactions.
	 * 
	 * @param parent the node to decorate
	 * @param payer the account that pays for the transactions that initialize the new accounts
	 * @param privateKeyOfPayer the private key of the account that pays for the transactions.
	 *                          It will be used to sign requests for installing the jars;
	 *                          the account must have enough coins for those transactions
	 * @param jars the jars to install in the node
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jars is rejected
	 * @throws TransactionException if some transaction that installs the jars fails
	 * @throws CodeExecutionException if some transaction that installs the jars throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signature algorithm of {@code parent} is not available
     */
	static NodeWithJars of(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, Path... jars) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new NodeWithJarsImpl(parent, payer, privateKeyOfPayer, jars);
	}
}