/**
 * 
 */
package io.takamaka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.nodes.CodeExecutionException;
import io.hotmoka.nodes.SideEffectsInViewMethodException;
import io.takamaka.tests.TakamakaTest;

class View extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private MemoryBlockchain blockchain;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = mkMemoryBlockchain(_1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, IOException {
		blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaCode(),
			bytesOf("view.jar"), blockchain.takamakaCode()));
	}

	@Test @DisplayName("install jar then call to View.no1() fails")
	void callNo1() throws TransactionException, IOException, CodeExecutionException {
		TransactionReference view = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaCode(),
			bytesOf("view.jar"), blockchain.takamakaCode()));

		Classpath classpath = new Classpath(view, true);
		StorageReference c = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(blockchain.account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.errors.view.C")));

		TakamakaTest.throwsTransactionExceptionWithCause(NoSuchMethodException.class, () -> 
			blockchain.runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(blockchain.account(0), _20_000, BigInteger.ONE, new Classpath(view, true),
				new NonVoidMethodSignature("io.takamaka.tests.errors.view.C", "no1", BasicTypes.INT, BasicTypes.INT, BasicTypes.INT),
				c, new IntValue(13), new IntValue(17))));
	}

	@Test @DisplayName("install jar then call to View.no2() fails")
	void callNo2() throws TransactionException, IOException, CodeExecutionException {
		TransactionReference view = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaCode(),
			bytesOf("view.jar"), blockchain.takamakaCode()));

		Classpath classpath = new Classpath(view, true);
		StorageReference c = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(blockchain.account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.errors.view.C")));

		TakamakaTest.throwsTransactionExceptionWithCause(SideEffectsInViewMethodException.class, () -> 
			blockchain.runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(blockchain.account(0), _20_000, BigInteger.ONE, new Classpath(view, true),
				new NonVoidMethodSignature("io.takamaka.tests.errors.view.C", "no2", BasicTypes.INT, BasicTypes.INT, BasicTypes.INT),
				c, new IntValue(13), new IntValue(17))));
	}

	@Test @DisplayName("install jar then call to View.yes() succeeds")
	void callYes() throws TransactionException, IOException, CodeExecutionException {
		TransactionReference view = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaCode(),
			bytesOf("view.jar"), blockchain.takamakaCode()));

		Classpath classpath = new Classpath(view, true);
		StorageReference c = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(blockchain.account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.errors.view.C")));

		blockchain.runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(blockchain.account(0), _20_000, BigInteger.ONE, new Classpath(view, true),
			new NonVoidMethodSignature("io.takamaka.tests.errors.view.C", "yes", BasicTypes.INT, BasicTypes.INT, BasicTypes.INT),
			c, new IntValue(13), new IntValue(17)));
	}
}