/**
 * 
 */
package io.hotmoka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for the deserialization of a cyclic data structure.
 */
class Cycle extends TakamakaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("cycle.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(BigInteger.valueOf(100_000L));
	}

	@Test @DisplayName("new Cycle().foo() == 42")
	void callFoo() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference cycle = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(),
			new ConstructorSignature("io.hotmoka.examples.cycle.Cycle"));

		IntValue result = (IntValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(),
			new NonVoidMethodSignature("io.hotmoka.examples.cycle.Cycle", "foo", BasicTypes.INT), cycle);

		assertEquals(42, result.value);
	}
}