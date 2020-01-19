package takamaka.tests.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.takamaka.code.blockchain.CodeExecutionException;
import io.takamaka.code.blockchain.TransactionException;
import io.takamaka.code.memory.InitializedMemoryBlockchain;

class LegalCall2 {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);
	private static final ClassType C = new ClassType("io.takamaka.tests.errors.legalcall2.C");

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedMemoryBlockchain(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"), _1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, CodeExecutionException, IOException {
		blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/legalcall2.jar")), blockchain.takamakaBase));
	}

	@Test @DisplayName("new C().test(); toString() == \"53331\"")
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