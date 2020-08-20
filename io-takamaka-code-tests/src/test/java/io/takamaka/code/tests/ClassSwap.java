/**
 * 
 */
package io.takamaka.code.tests;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.DeserializationError;

/**
 * A test for the creation of classes with the same name but from different jars.
 */
class ClassSwap extends TakamakaTest {

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	private static final ConstructorSignature CONSTRUCTOR_C = new ConstructorSignature("C");

	private static final MethodSignature GET = new NonVoidMethodSignature("C", "get", BasicTypes.INT);

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The only account of the blockchain.
	 */
	private StorageReference account;

	/**
	 * The private key of {@linkplain #account}.
	 */
	private PrivateKey key;

	/**
	 * The classpath for the class C whose method get() yields 13.
	 */
	private TransactionReference classpathC13;

	/**
	 * The classpath for the class C whose method get() yields 17.
	 */
	private TransactionReference classpathC17;

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(ALL_FUNDS);
		account = account(0);
		key = privateKey(0);

		classpathC13 = addJarStoreTransaction
			(key, account, _20_000, BigInteger.ONE, takamakaCode(), Files.readAllBytes(Paths.get("jars/c13.jar")), takamakaCode());

		classpathC17 = addJarStoreTransaction
			(key, account, _20_000, BigInteger.ONE, takamakaCode(), Files.readAllBytes(Paths.get("jars/c17.jar")), takamakaCode());
	}

	@Test @DisplayName("c13 new/get works in its classpath")
	void testC13() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c13 = addConstructorCallTransaction(key, account, _10_000, BigInteger.ONE, classpathC13, CONSTRUCTOR_C);
		IntValue get = (IntValue) addInstanceMethodCallTransaction(key, account, _10_000, BigInteger.ONE, classpathC13, GET, c13);

		assertSame(13, get.value);
	}

	@Test @DisplayName("c17 new/get works in its classpath")
	void testC17() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c17 = addConstructorCallTransaction(key, account, _10_000, BigInteger.ONE, classpathC17, CONSTRUCTOR_C);
		IntValue get = (IntValue) addInstanceMethodCallTransaction(key, account, _10_000, BigInteger.ONE, classpathC17, GET, c17);

		assertSame(17, get.value);
	}

	@Test @DisplayName("c13 new/get fails if classpath changed")
	void testC13SwapC17() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c13 = addConstructorCallTransaction(key, account, _10_000, BigInteger.ONE, classpathC13, CONSTRUCTOR_C);

		// the following call should fail since c13 was created from another jar
		throwsTransactionExceptionWithCause(DeserializationError.class, () ->
			addInstanceMethodCallTransaction(key, account, _10_000, BigInteger.ONE, classpathC17, GET, c13)
		);
	}
}