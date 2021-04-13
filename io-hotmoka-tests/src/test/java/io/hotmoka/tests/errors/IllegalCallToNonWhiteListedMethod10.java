package io.hotmoka.tests.errors;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.hotmoka.tests.TakamakaTest;

class IllegalCallToNonWhiteListedMethod10 extends TakamakaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("illegalcalltononwhitelistedmethod10.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("C.foo()")
	void installJar() {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			addStaticMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), new VoidMethodSignature(new ClassType("io.hotmoka.examples.errors.illegalcalltononwhitelistedmethod10.C"), "foo"))
		);
	}
}