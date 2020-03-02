package io.takamaka.code.memory.internal;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.CodeExecutionException;
import io.takamaka.code.memory.InitializedMemoryBlockchain;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 * It provides support for the creation of a given number of initial accounts.
 */
public class InitializedMemoryBlockchainImpl extends MemoryBlockchainImpl implements InitializedMemoryBlockchain {

	/**
	 * The reference, in the blockchain, where the base Takamaka classes have been installed.
	 */
	private final Classpath takamakaCode;

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	/**
	 * Builds a blockchain in disk memory and initializes user accounts
	 * with the given initial funds.
	 * 
	 * @param takamakaBasePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.takamaka.code.memory.InitializedMemoryBlockchain#takamakaCode}
	 * @param funds the initial funds of the accounts that are created
	 * @throws IOException if a disk error occurs
	 * @throws TransactionException if some transaction for initialization fails
	 * @throws CodeExecutionException if some transaction for initialization throws an exception
	 */
	public InitializedMemoryBlockchainImpl(Path takamakaBasePath, BigInteger... funds) throws IOException, TransactionException, CodeExecutionException {
		super(Paths.get("chain"));

		TransactionReference takamaka_base = addJarStoreInitialTransaction
			(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaBasePath)));
		this.takamakaCode = new Classpath(takamaka_base, false);

		// we compute the total amount of funds needed to create the accounts:
		// we do not start from 0 since we need some gas to create the accounts, below
		BigInteger sum = BigInteger.valueOf(1000000000L * funds.length);
		for (BigInteger fund: funds)
			sum = sum.add(fund);

		StorageReference gamete = addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaCode, sum));

		// let us create the accounts
		this.accounts = new StorageReference[funds.length];
		BigInteger gas = BigInteger.valueOf(10000); // enough for creating an account
		for (int i = 0; i < accounts.length; i++)
			this.accounts[i] = addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, gas, BigInteger.ZERO, takamakaCode, new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER), new BigIntegerValue(funds[i])));
	}

	@Override
	public StorageReference account(int i) {
		return accounts[i];
	}

	@Override
	public Classpath takamakaCode() {
		return takamakaCode;
	}
}