package io.hotmoka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.hotmoka.tests.TakamakaTest;

class IllegalCallToNonWhiteListedMethod7 extends TakamakaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("illegalcalltononwhitelistedmethod7.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("C.foo()")
	void installJar() throws TransactionException, CodeExecutionException, IOException {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			addStaticMethodCallTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, jar(),
				new NonVoidMethodSignature("io.hotmoka.examples.errors.illegalcalltononwhitelistedmethod7.C", "foo", ClassType.STRING))
		);
	}
}