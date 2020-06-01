package io.hotmoka.nodes.views;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Optional;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.internal.NodeWithAccountsImpl;

/**
 * A node that provides access to a previously installed jar and to
 * a predefined set of accounts. This is a useful interface for writing tests.
 */
public interface NodeWithAccounts extends Node {

	/**
	 * Yields the reference, in the store of the node, where the a user jar has been installed, if any.
	 */
	Optional<TransactionReference> jar();

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in the store of the node. This is a {@link #io.takamaka.code.lang.TestExternallyOwnedAccount}}
	 *         or a {@link #io.takamaka.code.lang.TestRedGreenExternallyOwnedAccount}}
	 */
	StorageReference account(int i);

	/**
	 * Yields the private key for controlling the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return its private key
	 */
	PrivateKey privateKey(int i);

	/**
	 * Yields a decorated node initialized with the given jar and a set of accounts.
	 * 
	 * @param parent the node to decorate
	 * @param jar the jar to install in the node
	 * @param funds the initial funds of the accounts to create
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static NodeWithAccounts of(Node parent, PrivateKey privateKeyOfGamete, Path jar, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new NodeWithAccountsImpl(parent, privateKeyOfGamete, jar, false, funds);
	}

	/**
	 * Yields a decorated node initialized with the given jar and a set of red/green accounts.
	 * 
	 * @param parent the node to decorate
	 * @param jar the jar to install in the node
	 * @param funds the initial funds of the accounts to create; they are understood in pairs: green before red of each account
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static NodeWithAccounts ofRedGreen(Node parent, PrivateKey privateKeyOfGamete, Path jar, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new NodeWithAccountsImpl(parent, privateKeyOfGamete, jar, true, funds);
	}

	/**
	 * Yields a decorated node initialized with a set of accounts.
	 * 
	 * @param parent the node to decorate
	 * @param funds the initial funds of the accounts to create
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static NodeWithAccounts of(Node parent, PrivateKey privateKeyOfGamete, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new NodeWithAccountsImpl(parent, privateKeyOfGamete, null, false, funds);
	}

	/**
	 * Yields a decorated node initialized with a set of red/green accounts.
	 * 
	 * @param parent the node to decorate
	 * @param funds the initial funds of the accounts to create; they are understood in pairs: green before red of each account
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static NodeWithAccounts ofRedGreen(Node parent, PrivateKey privateKeyOfGamete, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new NodeWithAccountsImpl(parent, privateKeyOfGamete, null, true, funds);
	}
}