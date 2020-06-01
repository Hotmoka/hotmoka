/**
 * 
 */
package io.takamaka.tests;

import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.takamaka.code.constants.Constants;

/**
 * A test for a class that uses lambda expressions referring to entries.
 */
class Lambdas extends TakamakaTest {
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final ClassType LAMBDAS = new ClassType("io.takamaka.tests.lambdas.Lambdas");
	private static final ConstructorSignature CONSTRUCTOR_LAMBDAS = new ConstructorSignature("io.takamaka.tests.lambdas.Lambdas", ClassType.BIG_INTEGER);
	private static final BigInteger _100_000 = BigInteger.valueOf(100_000);
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference eoa;

	/**
	 * The private key of {@linkplain #eoa}.
	 */
	private PrivateKey key;

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("lambdas.jar", ALL_FUNDS);
		eoa = account(0);
		key = privateKey(0);
	}

	@Test @DisplayName("new Lambdas()")
	void createLambdas() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(key, eoa, _10_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
	}

	@Test @DisplayName("new Lambdas().invest(10)")
	void createLambdasThenInvest10() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
		addInstanceMethodCallTransaction(key, eoa, _100_000, panarea(1), jar(), new VoidMethodSignature(LAMBDAS, "invest", ClassType.BIG_INTEGER), lambdas, new BigIntegerValue(BigInteger.ONE));
	}

	@Test @DisplayName("new Lambdas().testLambdaWithThis()")
	void testLambdaWithThis() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
		addInstanceMethodCallTransaction(key, eoa, _100_000, panarea(1), jar(), new VoidMethodSignature(LAMBDAS, "testLambdaWithThis"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testLambdaWithoutThis()")
	void testLambdaWithoutThis() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
		addInstanceMethodCallTransaction(key, eoa, _100_000, BigInteger.ONE, jar(), new VoidMethodSignature(LAMBDAS, "testLambdaWithoutThis"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testLambdaWithoutThisGetStatic()")
	void testLambdaWithoutThisGetStatic() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
		addInstanceMethodCallTransaction(key, eoa, _100_000, BigInteger.ONE, jar(), new VoidMethodSignature(LAMBDAS, "testLambdaWithoutThisGetStatic"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntry()")
	void testMethodReferenceToEntry() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
		IntValue result = (IntValue) addInstanceMethodCallTransaction(key, eoa, _100_000, BigInteger.ONE, jar(), new NonVoidMethodSignature(LAMBDAS, "testMethodReferenceToEntry", INT), lambdas);

		assertEquals(11, result.value);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntryOfOtherClass()")
	void testMethodReferenceToEntryOfOtherClass() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
		addInstanceMethodCallTransaction(key, eoa, _100_000, BigInteger.ONE, jar(), new VoidMethodSignature(LAMBDAS, "testMethodReferenceToEntryOfOtherClass"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntrySameContract()")
	void testMethodReferenceToEntrySameContract() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(key, eoa, _100_000, BigInteger.ONE, jar(), new NonVoidMethodSignature(LAMBDAS, "testMethodReferenceToEntrySameContract", INT), lambdas)
		);
	}

	@Test @DisplayName("new Lambdas().testConstructorReferenceToEntry()")
	void testConstructorReferenceToEntry() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
		IntValue result = (IntValue) addInstanceMethodCallTransaction(key, eoa, _100_000, BigInteger.ONE, jar(), new NonVoidMethodSignature(LAMBDAS, "testConstructorReferenceToEntry", INT), lambdas);

		assertEquals(11, result.value);
	}

	@Test @DisplayName("new Lambdas().testConstructorReferenceToEntryPopResult()")
	void testConstructorReferenceToEntryPopResult() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
		addInstanceMethodCallTransaction(key, eoa, _100_000, BigInteger.ONE, jar(), new VoidMethodSignature(LAMBDAS, "testConstructorReferenceToEntryPopResult"), lambdas);
	}

	@Test @DisplayName("new Lambdas().whiteListChecks(13,1,1973)==7")
	void testWhiteListChecks() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
		IntValue result = (IntValue) addInstanceMethodCallTransaction(key, eoa, _100_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature(LAMBDAS, "whiteListChecks", INT, ClassType.OBJECT, ClassType.OBJECT, ClassType.OBJECT),
			lambdas, new BigIntegerValue(BigInteger.valueOf(13L)), new BigIntegerValue(BigInteger.valueOf(1L)), new BigIntegerValue(BigInteger.valueOf(1973L)));

		assertEquals(7, result.value);
	}

	@Test @DisplayName("new Lambdas().concatenation(\"hello\", \"hi\", self, 1973L, 13)==\"\"")
	void testConcatenation() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000));
		StringValue result = (StringValue) addInstanceMethodCallTransaction(key, eoa, _100_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature(LAMBDAS, "concatenation", ClassType.STRING, ClassType.STRING, ClassType.OBJECT, LAMBDAS, BasicTypes.LONG, INT),
			lambdas,
			new StringValue("hello"), new StringValue("hi"), lambdas, new LongValue(1973L), new IntValue(13));

		assertEquals("hellohian externally owned account197313", result.value);
	}
}