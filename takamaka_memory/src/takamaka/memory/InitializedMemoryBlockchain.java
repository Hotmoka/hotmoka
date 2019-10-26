package takamaka.memory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.GameteCreationTransactionRequest;
import takamaka.blockchain.request.JarStoreInitialTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.StorageReference;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * It provides support for the creation of a given number of initial accounts.
 */
public class InitializedMemoryBlockchain extends MemoryBlockchain {

	/**
	 * The reference, in the blockchain, where the base Takamaka classes have been installed.
	 */
	public final Classpath takamakaBase;

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	/**
	 * Builds a blockchain in disk memory and initializes user accounts
	 * with the given initial funds.
	 * 
	 * @param takamakaBasePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link takamaka.memory.InitializedMemoryBlockchain#takamakaBase}
	 * @param funds the initial funds of the accounts that are created
	 * @throws IOException if a disk error occurs
	 * @throws TransactionException if some transaction for initialization fails
	 * @throws CodeExecutionException if some transaction for initialization throws an exception
	 */
	public InitializedMemoryBlockchain(Path takamakaBasePath, BigInteger... funds) throws IOException, TransactionException, CodeExecutionException {
		super(Paths.get("chain"));

		TransactionReference takamaka_base = addJarStoreInitialTransaction
			(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaBasePath)));
		this.takamakaBase = new Classpath(takamaka_base, false);

		// we compute the total amount of funds needed to create the accounts:
		// we do not start from 0 since we need some gas to create the accounts, below
		BigInteger sum = BigInteger.valueOf(1000000000L * funds.length);
		for (BigInteger fund: funds)
			sum = sum.add(fund);

		StorageReference gamete = addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, sum));

		// let us create the accounts
		this.accounts = new StorageReference[funds.length];
		BigInteger gas = BigInteger.valueOf(10000); // enough for creating an account
		for (int i = 0; i < accounts.length; i++)
			this.accounts[i] = addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, gas, takamakaBase, new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER), new BigIntegerValue(funds[i])));
	}

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in blockchain
	 */
	public StorageReference account(int i) {
		return accounts[i];
	}
}