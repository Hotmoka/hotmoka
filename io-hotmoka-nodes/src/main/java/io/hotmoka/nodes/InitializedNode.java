package io.hotmoka.nodes;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Optional;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.internal.InitializedNodeImpl;

/**
 * A node that provides access to a previously installed jar and to
 * a predefined set of accounts. This is a useful interface for writing tests.
 */
public interface InitializedNode extends Node {

	/**
	 * Yields the reference, in the store of the node, where the a user jar has been installed, if any.
	 */
	Optional<Classpath> jar();

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in the store of the node. This is a {@link #io.takamaka.code.lang.TestExternallyOwnedAccount}}
	 */
	StorageReference account(int i);

	/**
	 * Yields a decorated node initialized with the given jar and a set of accounts.
	 * 
	 * @param parent the node to decorate
	 * @param payer the payer of the initialization transactions; if red/green accounts are being created,
	 *              then this must be a red/green externally owned account; otherwise, it can also be
	 *              a normal externally owned account
	 * @param jar the jar to install in the node
	 * @param funds the initial funds of the accounts to create
	 * @return the decorated node
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 */
	static InitializedNode of(Node parent, StorageReference payer, Path jar, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException {
		return new InitializedNodeImpl(parent, payer, jar, false, funds);
	}

	/**
	 * Yields a decorated node initialized with the given jar and a set of red/green accounts.
	 * 
	 * @param parent the node to decorate
	 * @param payer the payer of the initialization transactions; if red/green accounts are being created,
	 *              then this must be a red/green externally owned account; otherwise, it can also be
	 *              a normal externally owned account
	 * @param jar the jar to install in the node
	 * @param funds the initial funds of the accounts to create; they are understood in pairs: green before red of each account
	 * @return the decorated node
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 */
	static InitializedNode ofRedGreen(Node parent, StorageReference payer, Path jar, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException {
		return new InitializedNodeImpl(parent, payer, jar, true, funds);
	}

	/**
	 * Yields a decorated node initialized with a set of accounts.
	 * 
	 * @param parent the node to decorate
	 * @param payer the payer of the initialization transactions; if red/green accounts are being created,
	 *              then this must be a red/green externally owned account; otherwise, it can also be
	 *              a normal externally owned account
	 * @param funds the initial funds of the accounts to create
	 * @return the decorated node
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 */
	static InitializedNode of(Node parent, StorageReference payer, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException {
		return new InitializedNodeImpl(parent, payer, null, false, funds);
	}

	/**
	 * Yields a decorated node initialized with a set of red/green accounts.
	 * 
	 * @param parent the node to decorate
	 * @param payer the payer of the initialization transactions; if red/green accounts are being created,
	 *              then this must be a red/green externally owned account; otherwise, it can also be
	 *              a normal externally owned account
	 * @param funds the initial funds of the accounts to create; they are understood in pairs: green before red of each account
	 * @return the decorated node
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 */
	static InitializedNode ofRedGreen(Node parent, StorageReference payer, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException {
		return new InitializedNodeImpl(parent, payer, null, true, funds);
	}
}