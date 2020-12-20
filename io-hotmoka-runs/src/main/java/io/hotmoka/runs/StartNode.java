package io.hotmoka.runs;

import static java.math.BigInteger.ZERO;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.NodeService;
import io.hotmoka.network.NodeServiceConfig;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.Node.CodeSupplier;
import io.hotmoka.nodes.views.NodeWithAccounts;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.views.TendermintInitializedNode;
import io.takamaka.code.constants.Constants;

/**
 * Starts a node of a network of two Tendermint nodes.
 * 
 * Run for instance on the first (big) machine with:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartNode 2 2 modules/explicit/io-takamaka-code-1.0.0.jar
 * 
 * and on the second (small) machine with:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartNode 1 2
 */
public class StartNode {
	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final int TRANSFERS = 250;
	private static final int ACCOUNTS = 12;
	private static final NonVoidMethodSignature GET_BALANCE = new NonVoidMethodSignature(Constants.TEOA_NAME, "getBalance", ClassType.BIG_INTEGER);

	/**
	 * Initial green stake.
	 */
	private final static BigInteger GREEN = BigInteger.valueOf(999_999_999).pow(5);

	/**
	 * Initial red stake.
	 */
	private final static BigInteger RED = BigInteger.valueOf(999_999_999).pow(5);

	/**
	 * The nonce of each externally owned account used in the test.
	 */
	private final static Map<StorageReference, BigInteger> nonces = new HashMap<>();

	private static SignatureAlgorithm<NonInitialTransactionRequest<?>> signature;

	private static String chainId;

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder().setDelete(false).build();
		NodeServiceConfig networkConfig = new NodeServiceConfig.Builder().setSpringBannerModeOn(false).build();

		System.out.println("usage: THIS_PROGRAM n t [server|takamakaCode]");
		System.out.println("  runs the n-th (1 to t) node over t");
		System.out.println("  installs takamakaCode inside the node");
		System.out.println("  or starts a server");

		Integer n = Integer.valueOf(args[0]);
		Integer t = Integer.valueOf(args[1]);
		boolean server;
		Path jarOfTakamakaCode;
		if (args.length > 2) {
			if ("server".equals(args[2])) {
				server = true;
				jarOfTakamakaCode = null;
			}
			else {
				server = false;
				jarOfTakamakaCode = Paths.get(args[2]);
			}
		}
		else {
			server = false;
			jarOfTakamakaCode = null;
		}

		System.out.println("Starting node " + n + " of " + t);

		// we delete the blockchain directory
		deleteRecursively(config.dir);

		// we replace the blockchain directory with the initialized data for the node
		Files.createDirectories(config.dir);

		copyRecursively(Paths.get("io-hotmoka-runs").resolve(t + "-nodes").resolve("node" + (n - 1)), config.dir.resolve("blocks"));

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config);
			 NodeService service = server ? NodeService.of(networkConfig, blockchain) : null) {

			signature = blockchain.getSignatureAlgorithmForRequests();
			chainId = blockchain.getTendermintChainId();

			if (jarOfTakamakaCode != null) {
				System.out.println("Installing " + jarOfTakamakaCode + " in it");
				TendermintInitializedNode initializedView = TendermintInitializedNode.of(blockchain, jarOfTakamakaCode, Constants.MANIFEST_NAME, GREEN, RED);

				System.out.println("Creating " + ACCOUNTS + " accounts");

				BigInteger[] funds = Stream.generate(() -> _200_000)
					.limit(ACCOUNTS)
					.toArray(BigInteger[]::new);

				NodeWithAccounts viewWithAccounts = NodeWithAccounts.of(initializedView, initializedView.gamete(), initializedView.keysOfGamete().getPrivate(), funds);

				System.out.println("Generating " + TRANSFERS + " random money transfers");
				Random random = new Random();
				long start = System.currentTimeMillis();

				TransactionReference takamakaCode = viewWithAccounts.getTakamakaCode();

				CodeSupplier<?>[] futures = new CodeSupplier<?>[ACCOUNTS];
				int transfers = 0;
				while (transfers < TRANSFERS) {
					for (int num = 0; num < ACCOUNTS && transfers < TRANSFERS; num++, transfers++) {
						StorageReference from = viewWithAccounts.account(num);
						PrivateKey key = viewWithAccounts.privateKey(num);

						StorageReference to;
						do {
							to = viewWithAccounts.account(random.nextInt(ACCOUNTS));
						}
						while (to == from); // we want a different account than from

						int amount = 1 + random.nextInt(10);
						futures[num] = postTransferTransaction(viewWithAccounts, from, key, ZERO, takamakaCode, to, amount);
					}

					// we wait until the last group is committed
					for (CodeSupplier<?> future: futures)
						future.get();

					System.out.println("... " + transfers);
				}

				long time = System.currentTimeMillis() - start;
				System.out.println(TRANSFERS + " money transfer transactions in " + time + "ms [" + (TRANSFERS * 1000L / time) + " tx/s]");

				// we compute the sum of the balances of the accounts
				BigInteger sum = ZERO;
				for (int i = 0; i < ACCOUNTS; i++)
					sum = sum.add(((BigIntegerValue) runViewInstanceMethodCallTransaction(viewWithAccounts, viewWithAccounts.account(0), viewWithAccounts.privateKey(0), _10_000, ZERO, takamakaCode, GET_BALANCE, viewWithAccounts.account(i))).value);

				// checks that no money got lost in translation
				System.out.println(sum + " should be " + ACCOUNTS * 200_000);
			}

			while (true) {
				try {
					System.out.println(blockchain.getTakamakaCode());
				}
				catch (NoSuchElementException e) {
					System.out.println("takamakaCode is not set yet");
				}

				Thread.sleep(1000);
			}
		}
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	private static CodeSupplier<StorageValue> postTransferTransaction(Node node, StorageReference caller, PrivateKey key, BigInteger gasPrice, TransactionReference classpath, StorageReference receiver, int howMuch) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		BigInteger nonce = getNonceOf(node, caller, key, classpath);
		return node.postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(Signer.with(signature, key), caller, nonce, chainId, _10_000, gasPrice, classpath, CodeSignature.RECEIVE_INT, receiver, new IntValue(howMuch)));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	private static StorageValue runViewInstanceMethodCallTransaction(Node node, StorageReference caller, PrivateKey key, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(Signer.with(signature, key), caller, ZERO, "", gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Gets the nonce of the given account. It calls the {@code Account.nonce()} method.
	 * 
	 * @param account the account
	 * @param key the private key of the account
	 * @param classpath the path where the execution must be performed
	 * @return the nonce
	 * @throws TransactionException if the nonce cannot be found
	 */
	private static BigInteger getNonceOf(Node node, StorageReference account, PrivateKey key, TransactionReference classpath) throws TransactionRejectedException {
		try {
			BigInteger nonce = nonces.get(account);
			if (nonce != null)
				nonce = nonce.add(BigInteger.ONE);
			else
				// we ask the account: 10,000 units of gas should be enough to run the method
				nonce = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(Signer.with(signature, key), account, ZERO, "", BigInteger.valueOf(10_000), ZERO, classpath, new NonVoidMethodSignature(Constants.ACCOUNT_NAME, "nonce", ClassType.BIG_INTEGER), account))).value;

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