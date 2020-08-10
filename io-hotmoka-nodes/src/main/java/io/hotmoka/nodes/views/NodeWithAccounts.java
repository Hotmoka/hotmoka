package io.hotmoka.nodes.views;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.NoSuchElementException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.internal.NodeWithAccountsImpl;

/**
 * A node that provides access to a previously installed set of accounts.
 */
@ThreadSafe
public interface NodeWithAccounts extends Node {

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in the store of the node. This is a {@link io.takamaka.code.lang.TestExternallyOwnedAccount}}
	 *         or a {@link io.takamaka.code.lang.TestRedGreenExternallyOwnedAccount}}
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
	 * Yields a decorated node initialized with the given jar and a set of accounts.
	 * The gamete pays for the transactions.
	 * 
	 * @param parent the node to decorate
	 * @param privateKeyOfGamete the private key of the gamete, that is needed to sign requests for initializing the accounts;
	 *                           the gamete must have enough coins to initialize the required accounts
	 * @param funds the initial funds of the accounts to create
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that creates the accounts is rejected
	 * @throws TransactionException if some transaction that creates the accounts fails
	 * @throws CodeExecutionException if some transaction that creates the accounts throws an exception
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static NodeWithAccounts of(Node parent, PrivateKey privateKeyOfGamete, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new NodeWithAccountsImpl(parent, privateKeyOfGamete, false, funds);
	}

	/**
	 * Yields a decorated node initialized with a set of red/green accounts.
	 * The gamete pays for the transactions.
	 * 
	 * @param parent the node to decorate
	 * @param privateKeyOfGamete the private key of the gamete, that is needed to sign requests for initializing the accounts;
	 *                           the gamete must have enough coins to initialize the required accounts
	 * @param funds the initial funds of the accounts to create; they are understood in pairs: green before red of each account
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that creates the accounts is rejected
	 * @throws TransactionException if some transaction that creates the accounts fails
	 * @throws CodeExecutionException if some transaction that creates the accounts throws an exception
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static NodeWithAccounts ofRedGreen(Node parent, PrivateKey privateKeyOfGamete, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new NodeWithAccountsImpl(parent, privateKeyOfGamete, true, funds);
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
		return new NodeWithAccountsImpl(parent, payer, privateKeyOfPayer, false, funds);
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
		return new NodeWithAccountsImpl(parent, payer, privateKeyOfPayer, true, funds);
	}
}