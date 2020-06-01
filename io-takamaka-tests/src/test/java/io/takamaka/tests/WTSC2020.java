/**
 * 
 */
package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import io.hotmoka.beans.values.StringValue;

/**
 * A test for the simple pyramid contract, used at the WTSC2020 workshop.
 */
class WTSC2020 extends TakamakaTest {
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final BigIntegerValue MINIMUM_INVESTMENT = new BigIntegerValue(BigInteger.valueOf(10_000L));
	private static final ClassType SIMPLE_PYRAMID = new ClassType("io.takamaka.tests.wtsc2020.SimplePyramid");
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = new ConstructorSignature(SIMPLE_PYRAMID, ClassType.BIG_INTEGER);
	private static final MethodSignature INVEST = new VoidMethodSignature(SIMPLE_PYRAMID, "invest", ClassType.BIG_INTEGER);
	private static final MethodSignature GET_BALANCE = new NonVoidMethodSignature(ClassType.TEOA, "getBalance", ClassType.BIG_INTEGER);
	private static final MethodSignature MOST_FREQUENT_INVESTOR = new NonVoidMethodSignature(SIMPLE_PYRAMID, "mostFrequentInvestor", ClassType.PAYABLE_CONTRACT);
	private static final MethodSignature MOST_FREQUENT_INVESTOR_CLASS = new NonVoidMethodSignature(SIMPLE_PYRAMID, "mostFrequentInvestorClass", ClassType.STRING);
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _20_000_000 = BigInteger.valueOf(20_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		// create a RAM simulation of a blockchain with 4 initial accounts
		setNode("wtsc2020.jar", _20_000_000, _20_000_000, _20_000_000, _20_000_000);
	}

	@Test @DisplayName("two investors do not get their investment back yet")
	void twoInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _10_000, BigInteger.ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		addInstanceMethodCallTransaction(privateKey(1), account(1), _10_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks its balance
		BigIntegerValue balance0 = (BigIntegerValue) runViewInstanceMethodCallTransaction(privateKey(0), account(0), _10_000, BigInteger.ZERO, jar(), GET_BALANCE, account(0));

		// no money back yet
		assertEquals(balance0.value, BigInteger.valueOf(19_990_000));
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _10_000, BigInteger.ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		postInstanceMethodCallTransaction(privateKey(1), account(1), _10_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(2) becomes the third investor
		addInstanceMethodCallTransaction(privateKey(2), account(2), _20_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks its balance
		BigIntegerValue balance0 = (BigIntegerValue) runViewInstanceMethodCallTransaction(privateKey(0), account(0), _10_000, BigInteger.ZERO, jar(), GET_BALANCE, account(0));

		// the money is back!
		assertEquals(balance0.value, BigInteger.valueOf(20_006_666));
	}

	@Test @DisplayName("three investors then check most frequent investor class")
	void mostFrequentInvestorClass() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		postInstanceMethodCallTransaction(privateKey(1), account(1), _10_000, BigInteger.ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(2) becomes the third investor
		postInstanceMethodCallTransaction(privateKey(2), account(2), _20_000, BigInteger.ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(1) invests again and becomes the most frequent investor
		addInstanceMethodCallTransaction(privateKey(1), account(1), _10_000, BigInteger.ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks which is the most frequent investor class
		StringValue result = (StringValue) runViewInstanceMethodCallTransaction(privateKey(0), account(0), _10_000, BigInteger.ONE, jar(), MOST_FREQUENT_INVESTOR_CLASS, pyramid);

		assertEquals(ClassType.TEOA.name, result.value);
	}

	@Disabled
	@Test @DisplayName("three investors then check most frequent investor and fails")
	void mostFrequentInvestor() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		postInstanceMethodCallTransaction(privateKey(1), account(1), _10_000, BigInteger.ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(2) becomes the third investor
		postInstanceMethodCallTransaction(privateKey(2), account(2), _20_000, BigInteger.ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(1) invests again and becomes the most frequent investor
		addInstanceMethodCallTransaction(privateKey(1), account(1), _10_000, BigInteger.ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks who is the most frequent investor
		runViewInstanceMethodCallTransaction(privateKey(0), account(0), _10_000, BigInteger.ONE, jar(), MOST_FREQUENT_INVESTOR, pyramid);
	}
}