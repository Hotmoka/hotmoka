package io.hotmoka.tests.errors;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
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
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.tests.TakamakaTest;

class LegalCall3 extends TakamakaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("legalcall3.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("C.test() == false")
	void callTest() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		BooleanValue result = (BooleanValue) addStaticMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature("io.hotmoka.examples.errors.legalcall3.C", "test", BasicTypes.BOOLEAN));

		assertFalse(result.value);
	}
}