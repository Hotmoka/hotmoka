package io.takamaka.code.engine;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.Node.CodeSupplier;
import io.hotmoka.nodes.Node.JarSupplier;

/**
 * Initialization procedures for a node. It installs some jars in the node and
 * creates some accounts, that can be later used to operate with the node.
 * The typical use of this class is in the constructors of
 * implementations of {@linkplain io.hotmoka.nodes.InitializedNode}, in order to
 * install initial jars and create initial accounts in the node.
 */
public final class Initialization {

	/**
	 * The reference, in the node, where the base Takamaka classes have been installed.
	 */
	public final Classpath takamakaCode;

	/**
	 * The classpath of a user jar that has been installed, if any.
	 */
	public final Classpath jar;

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	/**
	 * The method of red/green contracts to send red coins.
	 */
	private final static VoidMethodSignature RECEIVE_RED = new VoidMethodSignature(ClassType.RGPAYABLE_CONTRACT, "receiveRed", ClassType.BIG_INTEGER);

	/**
	 * The constructor of an externally owned account.
	 */
	private final static ConstructorSignature TEOA_CONSTRUCTOR = new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER);

	/**
	 * The constructor of an externally owned account with red/green funds.
	 */
	private final static ConstructorSignature TRGEOA_CONSTRUCTOR = new ConstructorSignature(ClassType.TRGEOA, ClassType.BIG_INTEGER);

	/**
	 * Creates initialization data for a node.
	 * 
	 * @param node the node
	 * @param takamakaCodePath the path of the jar with the basic Takamaka classes; this will be installed in the node
	 * @param jar the path of an extra jar to install in the node, if any; this is useful for testing, for installing
	 *            a first jar in the node
	 * @param redGreen true if and only if red/green externally owned accounts must be created; otherwise, normal
	 *                 externally owned accounts are created
	 * @param funds the initial funds of the accounts to create; for red/green accounts, they must be understood in pairs,
	 *              each pair for the green/red initial funds of each account (green before red)
	 * @throws Exception if some of the transactions that install jars or create accounts fails
	 */
	public Initialization(Node node, Path takamakaCodePath, Path jar, boolean redGreen, BigInteger... funds) throws Exception {
		this.takamakaCode = new Classpath(node.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCodePath))), false);

		StorageReference gamete;
		if (redGreen) {
			// we compute the total amount of red/green funds needed to create the accounts
			BigInteger green = IntStream.iterate(0, i -> i < funds.length, i -> i + 2)
				.mapToObj(i -> funds[i]).reduce(ZERO, BigInteger::add);

			BigInteger red = IntStream.iterate(1, i -> i < funds.length, i -> i + 2)
				.mapToObj(i -> funds[i]).reduce(ZERO, BigInteger::add);

			gamete = node.addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(takamakaCode, green, red));
		}
		else {
			// we compute the total amount of funds needed to create the accounts
			BigInteger sum = Stream.of(funds).reduce(ZERO, BigInteger::add);
			gamete = node.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaCode, sum));
		}

		BigInteger nonce = ZERO;
		JarSupplier jarSupplier;

		if (jar != null) {
			jarSupplier = node.postJarStoreTransaction(new JarStoreTransactionRequest(gamete, nonce, BigInteger.valueOf(1_000_000), ZERO, takamakaCode, Files.readAllBytes(jar), takamakaCode));
			nonce = nonce.add(ONE);
		}
		else
			jarSupplier = null;

		// we create the accounts
		BigInteger gas = BigInteger.valueOf(10_000); // enough for creating an account
		List<CodeSupplier<StorageReference>> accounts = new ArrayList<>();

		if (redGreen)
			for (int i = 0; i < funds.length; i += 2, nonce = nonce.add(ONE))
				// the constructor provides the green coins
				accounts.add(node.postConstructorCallTransaction(new ConstructorCallTransactionRequest
					(gamete, nonce, gas, ZERO, takamakaCode, TRGEOA_CONSTRUCTOR, new BigIntegerValue(funds[i]))));
		else
			for (BigInteger fund: funds) {
				accounts.add(node.postConstructorCallTransaction(new ConstructorCallTransactionRequest
					(gamete, nonce, gas, ZERO, takamakaCode, TEOA_CONSTRUCTOR, new BigIntegerValue(fund))));

				nonce = nonce.add(ONE);
			}

		int i = 0;
		this.accounts = new StorageReference[redGreen ? funds.length / 2 : funds.length];
		for (CodeSupplier<StorageReference> account: accounts) {
			// then we add the red coins
			this.accounts[i] = account.get();

			if (redGreen) {
				node.postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, nonce, gas, ZERO, takamakaCode,
					RECEIVE_RED, this.accounts[i], new BigIntegerValue(funds[1 + i * 2])));

				nonce = nonce.add(ONE);
			}

			i++;
		}

		this.jar = jarSupplier != null ? new Classpath(jarSupplier.get(), true) : null;
	}

	/**
	 * Yields the accounts that have been created.
	 * 
	 * @return the accounts
	 */
	public Stream<StorageReference> accounts() {
		return Stream.of(accounts);
	}
}