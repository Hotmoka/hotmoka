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

package io.hotmoka.helpers;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.helpers.api.AccountsNode;
import io.hotmoka.helpers.internal.AccountsNodeImpl;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.takamaka.code.constants.Constants;

/**
 * Providers of nodes that give access to a previously installed set of accounts.
 */
@ThreadSafe
public class AccountsNodes {

	private AccountsNodes() {}

	/**
	 * Yields a node initialized with a set of accounts.
	 * An account is provided, that pays for the transactions.
	 * 
	 * @param parent the node to decorate
	 * @param payer the account that pays for the transactions that initialize the new accounts
	 * @param privateKeyOfPayer the private key of the account that pays for the transactions.
	 *                          It will be used to sign requests for initializing the accounts;
	 *                          the account must have enough coins to initialize the required accounts
	 * @param funds the initial funds of the accounts to create
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that creates the accounts is rejected
	 * @throws TransactionException if some transaction that creates the accounts fails
	 * @throws CodeExecutionException if some transaction that creates the accounts throws an exception
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the payer uses an unknown signature algorithm
	 * @throws ClassNotFoundException if the class of the payer cannot be determined
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
     */
	public static AccountsNode of(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException, NodeException, UnknownReferenceException, TimeoutException, InterruptedException {
		return new AccountsNodeImpl(parent, payer, privateKeyOfPayer, Constants.EXTERNALLY_OWNED_ACCOUNTS_NAME, parent.getTakamakaCode(), false, funds);
	}

	/**
	 * Yields a node initialized with a set of accounts.
	 * An account is provided, that pays for the transactions.
	 * 
	 * @param parent the node to decorate
	 * @param payer the account that pays for the transactions that initialize the new accounts
	 * @param privateKeyOfPayer the private key of the account that pays for the transactions.
	 *                          It will be used to sign requests for initializing the accounts;
	 *                          the account must have enough coins to initialize the required accounts
	 * @param containerClassName the fully-qualified name of the class that must be used to contain the accounts;
	 *                           this must be {@code io.takamaka.code.lang.Accounts} or subclass
	 * @param classpath the classpath where {@code containerClassName} must be resolved
	 * @param funds the initial funds of the accounts to create
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that creates the accounts is rejected
	 * @throws TransactionException if some transaction that creates the accounts fails
	 * @throws CodeExecutionException if some transaction that creates the accounts throws an exception
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signature algorithm of {@code parent} is not available
	 * @throws ClassNotFoundException if the class of the payer cannot be determined
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NoSuchElementException if the node is not properly initialized
	 * @throws UnknownReferenceException if {@code payer} cannot be found in the node
     */
	public static AccountsNode of(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, String containerClassName, TransactionReference classpath, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException, NodeException, NoSuchElementException, TimeoutException, InterruptedException, UnknownReferenceException {
		return new AccountsNodeImpl(parent, payer, privateKeyOfPayer, containerClassName, classpath, false, funds);
	}

	/**
	 * Yields a node initialized with a set accounts, providing both green and red coins.
	 * An account is specified, that pays for the transactions.
	 * 
	 * @param parent the node to decorate
	 * @param payer the account that pays for the transactions that initialize the new accounts
	 * @param privateKeyOfPayer the private key of the account that pays for the transactions.
	 *                          It will be used to sign requests for initializing the accounts;
	 *                          the account must have enough coins to initialize the required accounts
	 * @param funds the initial funds of the accounts to create; they are understood in pairs: green before red of each account
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that creates the accounts is rejected
	 * @throws TransactionException if some transaction that creates the accounts fails
	 * @throws CodeExecutionException if some transaction that creates the accounts throws an exception
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signature algorithm of {@code parent} is not available
	 * @throws ClassNotFoundException if the class of the payer cannot be determined
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws UnknownReferenceException if {@code payer} cannot be found in the node
     */
	public static AccountsNode ofGreenRed(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchElementException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		return new AccountsNodeImpl(parent, payer, privateKeyOfPayer, Constants.EXTERNALLY_OWNED_ACCOUNTS_NAME, parent.getTakamakaCode(), true, funds);
	}
}