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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.constants.Constants;
import io.hotmoka.nodes.Node;
import io.hotmoka.views.internal.NodeWithAccountsImpl;

/**
 * A node that provides access to a previously installed set of accounts.
 */
@ThreadSafe
public interface NodeWithAccounts extends Node {

	/**
	 * Yields the accounts.
	 * 
	 * @return the references to the accounts. This is an instance of {@link io.takamaka.code.lang.Accounts}
	 */
	Stream<StorageReference> accounts();

	/**
	 * Yields the private keys for controlling the accounts.
	 * 
	 * @return the private keys, in the same order as {@link #accounts()}
	 */
	Stream<PrivateKey> privateKeys();

	/**
	 * Yields the container of the accounts that have been created.
	 * 
	 * @return the container. This is an instance of {@code io.takamaka.code.lang.Accounts}
	 */
	StorageReference container();

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in the store of the node. This is an {@link io.takamaka.code.lang.ExternallyOwnedAccount}
	 * @throws NoSuchElementException if the {@code i}th account does not exist
	 */
	StorageReference account(int i) throws NoSuchElementException;

	/**
	 * Yields the private key for controlling the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return its private key
	 * @throws NoSuchElementException if the {@code i}th account does not exist
	 */
	PrivateKey privateKey(int i) throws NoSuchElementException;

	/**
	 * Yields a decorated node initialized with a set of accounts.
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
	 * @throws NoSuchAlgorithmException if the signature algorithm of {@code parent} is not available
	 * @throws ClassNotFoundException 
     */
	static NodeWithAccounts of(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchElementException, ClassNotFoundException {
		return new NodeWithAccountsImpl(parent, payer, privateKeyOfPayer, Constants.EXTERNALLY_OWNED_ACCOUNTS_NAME, parent.getTakamakaCode(), false, funds);
	}

	/**
	 * Yields a decorated node initialized with a set of accounts.
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
	 * @throws ClassNotFoundException 
     */
	static NodeWithAccounts of(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, String containerClassName, TransactionReference classpath, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException {
		return new NodeWithAccountsImpl(parent, payer, privateKeyOfPayer, containerClassName, classpath, false, funds);
	}

	/**
	 * Yields a decorated node initialized with a set accounts, providing both green and red coins.
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
	 * @throws ClassNotFoundException 
     */
	static NodeWithAccounts ofGreenRed(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchElementException, ClassNotFoundException {
		return new NodeWithAccountsImpl(parent, payer, privateKeyOfPayer, Constants.EXTERNALLY_OWNED_ACCOUNTS_NAME, parent.getTakamakaCode(), true, funds);
	}
}