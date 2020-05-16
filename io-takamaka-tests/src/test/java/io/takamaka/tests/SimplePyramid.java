/**
 * 
 */
package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for the simple pyramid contract.
 */
class SimplePyramid extends TakamakaTest {
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final BigIntegerValue MINIMUM_INVESTMENT = new BigIntegerValue(BigInteger.valueOf(10_000L));
	private static final ClassType SIMPLE_PYRAMID = new ClassType("io.takamaka.tests.ponzi.SimplePyramid");
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = new ConstructorSignature(SIMPLE_PYRAMID, ClassType.BIG_INTEGER);
	private static final MethodSignature INVEST = new VoidMethodSignature(SIMPLE_PYRAMID, "invest", ClassType.BIG_INTEGER);
	private static final MethodSignature GET_BALANCE = new NonVoidMethodSignature(ClassType.TEOA, "getBalance", ClassType.BIG_INTEGER);
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("ponzi.jar", BigInteger.valueOf(200_000L), BigInteger.valueOf(200_000L), BigInteger.valueOf(200_000L));
	}

	@Test @DisplayName("two investors do not get investment back yet")
	void twoInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		StorageReference pyramid = addConstructorCallTransaction(account(0), _10_000, BigInteger.ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(account(1), _10_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		BigIntegerValue balance0 = (BigIntegerValue) runViewInstanceMethodCallTransaction(account(0), _10_000, BigInteger.ZERO, jar(), GET_BALANCE, account(0));
		assertTrue(balance0.value.compareTo(BigInteger.valueOf(190_000)) <= 0);
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		StorageReference pyramid = addConstructorCallTransaction(account(0), _10_000, BigInteger.ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		postInstanceMethodCallTransaction(account(1), _10_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(account(2), _20_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		BigIntegerValue balance0 = (BigIntegerValue) runViewInstanceMethodCallTransaction(account(0), _10_000, BigInteger.ZERO, jar(), GET_BALANCE, account(0));
		assertTrue(balance0.value.compareTo(BigInteger.valueOf(201_000)) >= 0);
	}
}