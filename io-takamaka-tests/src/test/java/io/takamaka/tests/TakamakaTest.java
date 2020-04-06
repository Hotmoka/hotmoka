package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.nodes.AsynchronousNode;
import io.hotmoka.nodes.AsynchronousNode.CodeExecutionFuture;
import io.hotmoka.nodes.AsynchronousNode.JarStoreFuture;
import io.hotmoka.nodes.NodeWithAccounts;
import io.hotmoka.nodes.SynchronousNode;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.verification.VerificationException;

public abstract class TakamakaTest {

	/**
	 * The node under test. This is recreated before each test.
	 */
	private NodeWithAccounts node;

	/**
	 * The nonce of each externally owned account used in the test.
	 */
	private Map<StorageReference, BigInteger> nonces = new HashMap<>();

	public interface TestBody {
		public void run() throws Exception;
	}

	/**
	 * Change in order to specify the default blockchain to use in tests, when not
	 * explicitly required otherwise.
	 */
	protected final void mkBlockchain(BigInteger... coins) throws Exception {
		//Config config = new Config(Paths.get("chain"), 26657, 26658);
		//node = TendermintBlockchain.of(config, Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), coins);
		node = MemoryBlockchain.of(Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), coins);
	}

	protected final void mkRedGreenBlockchain(BigInteger... coins) throws IOException, TransactionException, CodeExecutionException {
		//Config config = new Config(Paths.get("chain"), 26657, 26658);
		//node = TendermintBlockchain.ofRedGreen(config, Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), coins);
		node = MemoryBlockchain.ofRedGreen(Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), coins);
	}

	@AfterEach
	void afterEach() throws Exception {
		node.close();
	}

	protected final Classpath takamakaCode() {
		return node.takamakaCode();
	}

	protected final StorageReference account(int i) {
		return node.account(i);
	}

	protected final String getClassNameOf(StorageReference storageReference) {
		return node.getClassNameOf(storageReference);
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final TransactionReference addJarStoreTransaction(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, byte[] jar, Classpath... dependencies) throws TransactionException {
		if (node instanceof SynchronousNode) {
			BigInteger nonce = getNonceOf(caller, classpath);
			return ((SynchronousNode) node).addJarStoreTransaction(new JarStoreTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, jar, dependencies));
		}
		else
			throw new IllegalStateException("can only call addJarStoreTransaction() on a synchronous node");
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageReference addConstructorCallTransaction(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		if (node instanceof SynchronousNode) {
			BigInteger nonce = getNonceOf(caller, classpath);
			return ((SynchronousNode) node).addConstructorCallTransaction(new ConstructorCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, constructor, actuals));
		}
		else
			throw new IllegalStateException("can only call addConstructorCallTransaction() on a synchronous node");
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue addInstanceMethodCallTransaction(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		if (node instanceof SynchronousNode) {
			BigInteger nonce = getNonceOf(caller, classpath);
			return ((SynchronousNode) node).addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals));
		}
		else
			throw new IllegalStateException("can only call addInstanceMethodCallTransaction() on a synchronous node");
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue addStaticMethodCallTransaction(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		if (node instanceof SynchronousNode) {
			BigInteger nonce = getNonceOf(caller, classpath);
			return ((SynchronousNode) node).addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, method, actuals));
		}
		else
			throw new IllegalStateException("can only call addStaticMethodCallTransaction() on a synchronous node");
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runViewInstanceMethodCallTransaction(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		return node.runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(caller, BigInteger.ZERO, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runViewStaticMethodCallTransaction(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		return node.runViewStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(caller, BigInteger.ZERO, gasLimit, gasPrice, classpath, method, actuals));
	}

	protected final JarStoreFuture postJarStoreTransaction(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, byte[] jar, Classpath... dependencies) throws TransactionException {
		if (node instanceof AsynchronousNode) {
			BigInteger nonce = getNonceOf(caller, classpath);
			return ((AsynchronousNode) node).postJarStoreTransaction(new JarStoreTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, jar, dependencies));
		}
		else
			throw new IllegalStateException("can only call postJarStoreTransaction() on an asynchronous node");
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeExecutionFuture<StorageValue> postInstanceMethodCallTransaction(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException {
		if (node instanceof AsynchronousNode) {
			BigInteger nonce = getNonceOf(caller, classpath);
			return ((AsynchronousNode) node).postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, method, receiver, actuals));
		}
		else
			throw new IllegalStateException("can only call postInstanceMethodCallTransaction() on an asynchronous node");
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeExecutionFuture<StorageReference> postConstructorCallTransaction(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionException {
		if (node instanceof AsynchronousNode) {
			BigInteger nonce = getNonceOf(caller, classpath);
			return ((AsynchronousNode) node).postConstructorCallTransaction(new ConstructorCallTransactionRequest(caller, nonce, gasLimit, gasPrice, classpath, constructor, actuals));
		}
		else
			throw new IllegalStateException("can only call postConstructorCallTransaction() on an asynchronous node");
	}

	protected static byte[] bytesOf(String fileName) throws IOException {
		return Files.readAllBytes(Paths.get("../io-takamaka-examples/target/io-takamaka-examples-1.0-" + fileName));
	}

	protected static void throwsTransactionExceptionWithCause(Class<? extends Throwable> expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			if (e.getMessage().startsWith(expected.getName()))
				return;

			fail("wrong cause: expected " + expected.getName() + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionExceptionWithCause(String expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			if (e.getMessage().startsWith(expected))
				return;

			fail("wrong cause: expected " + expected + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionException(TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			return;
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}	

	protected static void throwsVerificationException(TestBody what) {
		throwsTransactionExceptionWithCause(VerificationException.class, what);
	}

	/**
	 * Gets the nonce of the given account. It calls the {@code Account.nonce()} method.
	 * 
	 * @param account the account
	 * @param classpath the path where the execution must be performed
	 * @return the nonce
	 * @throws TransactionException if the nonce cannot be found
	 */
	private BigInteger getNonceOf(StorageReference account, Classpath classpath) throws TransactionException {
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
			throw new TransactionException("cannot compute the nonce of " + account);
		}
	}
}