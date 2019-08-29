package takamaka.tests.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.NonVoidMethodSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.VoidMethodSignature;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StringValue;
import takamaka.memory.InitializedMemoryBlockchain;

class LegalCall2 {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);
	private static final ClassType C = new ClassType("takamaka.tests.errors.legalcall2.C");

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedMemoryBlockchain(Paths.get("../takamaka_runtime/dist/takamaka_base.jar"), _1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, CodeExecutionException, IOException {
		blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/legalcall2.jar")), blockchain.takamakaBase));
	}

	@Test @DisplayName("new C().test(); toString() ==")
	void newTestToString() throws TransactionException, CodeExecutionException, IOException {
		TransactionReference jar = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/legalcall2.jar")), blockchain.takamakaBase));

		Classpath classpath = new Classpath(jar, true);

		StorageReference c = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(blockchain.account(0), _20_000, classpath, new ConstructorSignature(C)));

		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(blockchain.account(0), _20_000, classpath, new VoidMethodSignature(C, "test"), c));

		StringValue result = (StringValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(blockchain.account(0), _20_000, classpath, new NonVoidMethodSignature(C, "toString", ClassType.STRING), c));

		assertEquals("53331", result.value);
	}
}