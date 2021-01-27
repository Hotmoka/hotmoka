/**
 * 
 */
package io.hotmoka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.values.IntValue;

/**
 * A test to check if class loaders correctly deal with a static method that calls another static method.
 */
class StaticFromStatic extends TakamakaTest {
	private static final BigInteger _100_000_000 = BigInteger.valueOf(100_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("staticfromstatic.jar", _100_000_000);
	}

	@Test @DisplayName("StaticFromStatic.foo() == 42")
	void callFoo() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		IntValue result = (IntValue) addStaticMethodCallTransaction(privateKey(0), account(0), BigInteger.valueOf(10_000), BigInteger.ONE, jar(),
			new NonVoidMethodSignature("io.hotmoka.tests.staticfromstatic.StaticFromStatic", "foo", BasicTypes.INT));

		assertEquals(42, result.value);
	}
}