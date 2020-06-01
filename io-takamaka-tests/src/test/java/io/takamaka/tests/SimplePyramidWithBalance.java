/**
 * 
 */
package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

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
 * A test for the simple pyramid with balance contract.
 */
class SimplePyramidWithBalance extends TakamakaTest {
	private static final BigInteger _50_000 = BigInteger.valueOf(50_000);
	private static final BigIntegerValue MINIMUM_INVESTMENT = new BigIntegerValue(BigInteger.valueOf(10_000L));
	private static final ClassType SIMPLE_PYRAMID = new ClassType("io.takamaka.tests.ponzi.SimplePyramidWithBalance");
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = new ConstructorSignature(SIMPLE_PYRAMID, ClassType.BIG_INTEGER);
	private static final MethodSignature INVEST = new VoidMethodSignature(SIMPLE_PYRAMID, "invest", ClassType.BIG_INTEGER);
	private static final MethodSignature WITHDRAW = new VoidMethodSignature(SIMPLE_PYRAMID, "withdraw");
	private static final MethodSignature GET_BALANCE = new NonVoidMethodSignature(ClassType.TEOA, "getBalance", ClassType.BIG_INTEGER);
	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("ponzi.jar", _200_000, _200_000, _200_000);
	}

	@Test @DisplayName("two investors do not get investment back yet")
	void twoInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		postInstanceMethodCallTransaction(privateKey(1), account(1), _50_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ZERO, jar(), WITHDRAW, pyramid);
		BigIntegerValue balance0 = (BigIntegerValue) runViewInstanceMethodCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ZERO, jar(), GET_BALANCE, account(0));
		assertTrue(balance0.value.compareTo(BigInteger.valueOf(190_000)) <= 0);
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		postInstanceMethodCallTransaction(privateKey(1), account(1), _50_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		postInstanceMethodCallTransaction(privateKey(2), account(2), _50_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ZERO, jar(), WITHDRAW, pyramid);
		BigIntegerValue balance0 = (BigIntegerValue) runViewInstanceMethodCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ZERO, jar(), GET_BALANCE, account(0));
		assertTrue(balance0.value.compareTo(BigInteger.valueOf(201_000)) > 0);
	}
}