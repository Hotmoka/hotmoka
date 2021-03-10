package io.hotmoka.nodes.views;

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
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.internal.NodeWithAccountsImpl;
import io.takamaka.code.constants.Constants;

/**
 * A node that provides access to a previously installed set of accounts.
 */
@ThreadSafe
public interface NodeWithAccounts extends Node {

	/**
	 * Yields the accounts.
	 * 
	 * @return the references to the accounts. This is an instance of {@link io.takamaka.code.lang.Accounts}
	 *         or {@link io.takamaka.code.lang.RedGreenAccounts}
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
	 *         or an {@link io.takamaka.code.lang.RedGreenExternallyOwnedAccount}
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
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static NodeWithAccounts of(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
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
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static NodeWithAccounts of(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, String containerClassName, TransactionReference classpath, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new NodeWithAccountsImpl(parent, payer, privateKeyOfPayer, containerClassName, classpath, false, funds);
	}

	/**
	 * Yields a decorated node initialized with a set of red/green accounts.
	 * An account is provided, that pays for the transactions.
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
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static NodeWithAccounts ofRedGreen(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new NodeWithAccountsImpl(parent, payer, privateKeyOfPayer, Constants.RED_GREEN_EXTERNALLY_OWNED_ACCOUNTS_NAME, parent.getTakamakaCode(), true, funds);
	}
}