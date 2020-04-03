package io.hotmoka.tendermint;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.AsynchronousNode;
import io.hotmoka.nodes.NodeWithAccounts;
import io.hotmoka.nodes.SynchronousNode;
import io.hotmoka.tendermint.internal.TendermintBlockchainImpl;

/**
 * An implementation of a blockchain that stores, sequentially, transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 * It provides support for the creation of a given number of initial accounts.
 */
public interface TendermintBlockchain extends AsynchronousNode, SynchronousNode, NodeWithAccounts, AutoCloseable {

	/**
	 * Yields a Tendermint blockchain and initializes user accounts with the given initial funds.
	 * This method spawns the Tendermint process on localhost and connects it to an ABCI application
	 * for handling its transactions. The blockchain gets deleted if it existed already at the given directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode}
	 * @param funds the initial funds of the accounts that are created
	 * @throws IOException if a disk error occurs
	 * @throws TransactionException if some transaction for initialization fails
	 * @throws CodeExecutionException if some transaction for initialization throws an exception
	 */
	static TendermintBlockchain of(Config config, Path takamakaCodePath, BigInteger... funds) throws IOException, TransactionException, CodeExecutionException {
		return new TendermintBlockchainImpl(config, takamakaCodePath, funds);
	}

	/**
	 * Yields a Tendermint blockchain and initializes it with the information already
	 * existing at its configuration directory. This method can be used to
	 * recover a blockchain already created in the past, with all its information.
	 * A Tendermint blockchain must have been already successfully created at
	 * its configuration directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @throws IOException if a disk error occurs
	 * @throws InterruptedException if the Java process has been interrupted while starting Tendermint
	 */
	static TendermintBlockchain of(Config config) throws IOException, InterruptedException {
		return new TendermintBlockchainImpl(config);
	}

	/**
	 * Yields the reference, in the blockchain, where the base Takamaka classes have been installed.
	 */
	Classpath takamakaCode();

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in blockchain. This is a {@link #io.takamaka.code.lang.TestExternallyOwnedAccount}}
	 */
	StorageReference account(int i);
}