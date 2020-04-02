package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

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
import io.hotmoka.memory.RedGreenMemoryBlockchain;
import io.hotmoka.nodes.NodeWithAccounts;
import io.hotmoka.nodes.SynchronousNode;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.verification.VerificationException;

public abstract class TakamakaTest {

	/**
	 * The node under test. This is recreated before each test.
	 */
	private NodeWithAccounts node;

	public interface TestBody {
		public void run() throws Exception;
	}

	protected final void mkMemoryBlockchain(BigInteger... coins) throws IOException, TransactionException, CodeExecutionException {
		node = MemoryBlockchain.of(Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), coins);
	}

	protected final void mkRedGreenMemoryBlockchain(BigInteger... coins) throws IOException, TransactionException, CodeExecutionException {
		node = RedGreenMemoryBlockchain.of(Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), coins);
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
			BigInteger nonce = getNonceOf(caller, gasLimit, gasPrice, classpath);
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
			BigInteger nonce = getNonceOf(caller, gasLimit, gasPrice, classpath);
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
			BigInteger nonce = getNonceOf(caller, gasLimit, gasPrice, classpath);
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
			BigInteger nonce = getNonceOf(caller, gasLimit, gasPrice, classpath);
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
	 * @param gasLimit the gas limit allowed to be spent
	 * @param gasPrice the cost of the unit of gas
	 * @param classpath the path where the execution must be performed
	 * @return the nonce
	 * @throws TransactionException if the nonce cannot be found
	 */
	private BigInteger getNonceOf(StorageReference account, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath) throws TransactionException {
		try {
			return ((BigIntegerValue) node.runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(account, BigInteger.ZERO, gasLimit, gasPrice, classpath, new NonVoidMethodSignature(Constants.ACCOUNT_NAME, "nonce", ClassType.BIG_INTEGER), account))).value;
		}
		catch (Exception e) {
			throw new TransactionException("cannot compute the nonce of " + account);
		}
	}
}