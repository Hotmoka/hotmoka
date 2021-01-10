package io.takamaka.code.tests.errors;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.takamaka.code.tests.TakamakaTest;

class IllegalCallToNonWhiteListedMethod8 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("illegalcalltononwhitelistedmethod8.jar", _1_000_000_000);
	}

	@Test @DisplayName("C.foo()")
	void installJar() throws TransactionException, CodeExecutionException, IOException {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			addStaticMethodCallTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, jar(),
				new NonVoidMethodSignature("io.takamaka.tests.errors.illegalcalltononwhitelistedmethod8.C", "foo", ClassType.STRING))
		);
	}
}