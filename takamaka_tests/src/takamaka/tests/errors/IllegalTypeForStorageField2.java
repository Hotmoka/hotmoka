/**
 * 
 */
package takamaka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.EnumValue;
import io.hotmoka.nodes.DeserializationError;
import io.takamaka.code.engine.CodeExecutionException;
import io.takamaka.code.memory.InitializedMemoryBlockchain;
import takamaka.tests.TakamakaTest;

class IllegalTypeForStorageField2 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedMemoryBlockchain(Paths.get("../distribution/dist/io-takamaka-code-1.0.jar"), _1_000_000_000);
	}

	@Test @DisplayName("store mutable enum into Object")
	void installJar() throws TransactionException, CodeExecutionException, IOException {
		TransactionReference jar = blockchain.addJarStoreTransaction
				(new JarStoreTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
				Files.readAllBytes(Paths.get("../takamaka_examples/dist/illegaltypeforstoragefield2.jar")), blockchain.takamakaBase));
		Classpath classpath = new Classpath(jar, true);

		throwsTransactionExceptionWithCause(DeserializationError.class, () ->
			blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, classpath,
				new ConstructorSignature("takamaka.tests.errors.illegaltypeforstoragefield2.C", ClassType.OBJECT),
				new EnumValue("takamaka.tests.errors.illegaltypeforstoragefield2.MyEnum", "FIRST")))
		);
	}
}