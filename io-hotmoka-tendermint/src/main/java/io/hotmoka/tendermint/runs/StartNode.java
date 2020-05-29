package io.hotmoka.tendermint.runs;

import static java.math.BigInteger.ZERO;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.TransferTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.nodes.InitializedNode;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.Node.CodeSupplier;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.takamaka.code.constants.Constants;

public class StartNode {
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final int TRANSFERS = 1500;
	private static final int ACCOUNTS = 4;
	private static final NonVoidMethodSignature GET_BALANCE = new NonVoidMethodSignature(Constants.TEOA_NAME, "getBalance", ClassType.BIG_INTEGER);

	/**
	 * The nonce of each externally owned account used in the test.
	 */
	private final static Map<StorageReference, BigInteger> nonces = new HashMap<>();

	public static void main(String[] args) throws Exception {
		Config config = new Config.Builder().setDelete(false).build();

		System.out.println("usage: THIS_PROGRAM n t takamakaCode");
		System.out.println("  runs the n-th (1 to t) node over t");
		System.out.println("  installs takamakaCode inside the node");

		Integer n = Integer.valueOf(args[0]);
		Integer t = Integer.valueOf(args[1]);
		Path takamakaCode;
		if (args.length > 2)
			takamakaCode = Paths.get(args[2]);
		else
			takamakaCode = null;

		System.out.println("Starting node " + n + " of " + t);
		if (takamakaCode != null)
			System.out.println("Installing " + takamakaCode + " in it");

		// we delete the blockchain directory
		deleteRecursively(config.dir);

		// we replace the blockchain directory with the initialized data for the node
		Files.createDirectories(config.dir);

		copyRecursively(Paths.get(t + "-nodes").resolve("node" + (n - 1)), config.dir.resolve("blocks"));

		if (takamakaCode != null) {
			try (TendermintBlockchain blockchain = TendermintBlockchain.of(config)) {
				StorageReference gamete = blockchain.addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(blockchain.getTakamakaCode(), BigInteger.valueOf(999_999_999), BigInteger.valueOf(999_999_999)));

				try (InitializedNode node = InitializedNode.of(blockchain, gamete, BigInteger.valueOf(200_000), BigInteger.valueOf(200_000), BigInteger.valueOf(200_000), BigInteger.valueOf(200_000))) {
					Random random = new Random();
					long start = System.currentTimeMillis();

					for (int i = 0; i < TRANSFERS; i++) {
						StorageReference from = node.account(random.nextInt(ACCOUNTS));

						StorageReference to;
						do {
							to = node.account(random.nextInt(ACCOUNTS));
						}
						while (to == from); // we want a different account than from

						int amount = 1 + random.nextInt(10);
						//System.out.println(amount + ": " + from + " -> " + to);
						if (i < TRANSFERS - 1)
							postTransferTransaction(node, from, ZERO, node.getTakamakaCode(), to, amount);
						else
							// the last transaction requires to wait until everything is committed
							addTransferTransaction(node, from, ZERO, node.getTakamakaCode(), to, amount);
					}

					long time = System.currentTimeMillis() - start;
					System.out.println(TRANSFERS + " money transfer transactions in " + time + "ms [" + (TRANSFERS * 1000L / time) + " tx/s]");

					// we compute the sum of the balances of the accounts
					BigInteger sum = ZERO;
					for (int i = 0; i < ACCOUNTS; i++)
						sum = sum.add(((BigIntegerValue) runViewInstanceMethodCallTransaction(node, node.account(0), _10_000, ZERO, node.getTakamakaCode(), GET_BALANCE, node.account(i))).value);

					// no money got lost in translation
					System.out.println(sum + " should be " + ACCOUNTS * 200_000);

					while (true) {
						System.out.println(node.getTakamakaCode());
						Thread.sleep(1000);
					}
				}
			}
		}
		else {
			try (TendermintBlockchain node = TendermintBlockchain.of(config)) {
				while (true) {
					System.out.println(node.getTakamakaCode());
					Thread.sleep(1000);
				}
			}
		}
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	private static CodeSupplier<StorageValue> postTransferTransaction(Node node, StorageReference caller, BigInteger gasPrice, TransactionReference classpath, StorageReference receiver, int howMuch) throws TransactionRejectedException {
		BigInteger nonce = getNonceOf(node, caller, classpath);
		return node.postInstanceMethodCallTransaction(new TransferTransactionRequest(caller, nonce, gasPrice, classpath, receiver, howMuch));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	private static void addTransferTransaction(Node node, StorageReference caller, BigInteger gasPrice, TransactionReference classpath, StorageReference receiver, int howMuch) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		BigInteger nonce = getNonceOf(node, caller, classpath);
		node.addInstanceMethodCallTransaction(new TransferTransactionRequest(caller, nonce, gasPrice, classpath, receiver, howMuch));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	private static StorageValue runViewInstanceMethodCallTransaction(Node node, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException {
		return node.runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(caller, BigInteger.ZERO, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Gets the nonce of the given account. It calls the {@code Account.nonce()} method.
	 * 
	 * @param account the account
	 * @param classpath the path where the execution must be performed
	 * @return the nonce
	 * @throws TransactionException if the nonce cannot be found
	 */
	private static BigInteger getNonceOf(Node node, StorageReference account, TransactionReference classpath) throws TransactionRejectedException {
		try {
			BigInteger nonce = nonces.get(account);
			if (nonce != null)
				nonce = nonce.add(BigInteger.ONE);
			else
				// we ask the account: 10,000 units of gas should be enough to run the method
				nonce = ((BigIntegerValue) node.runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(account, BigInteger.ZERO, BigInteger.valueOf(10_000), BigInteger.ZERO, classpath, new NonVoidMethodSignature(Constants.ACCOUNT_NAME, "nonce", ClassType.BIG_INTEGER), account))).value;

			nonces.put(account, nonce);
			return nonce;
		}
		catch (Exception e) {
			throw new TransactionRejectedException("cannot compute the nonce of " + account);
		}
	}

	/**
	 * Deletes the given directory, recursively.
	 * 
	 * @param dir the directory to delete
	 * @throws IOException if the directory or some of its subdirectories cannot be deleted
	 */
	private static void deleteRecursively(Path dir) throws IOException {
		if (Files.exists(dir))
			Files.walk(dir)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}

	private static void copyRecursively(Path src, Path dest) throws IOException {
	    try (Stream<Path> stream = Files.walk(src)) {
	        stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
	    }
	    catch (UncheckedIOException e) {
	    	throw e.getCause();
	    }
	}

	private static void copy(Path source, Path dest) {
		try {
			Files.copy(source, dest);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}