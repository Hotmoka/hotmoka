package io.takamaka.tests.errors;

import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.takamaka.tests.TakamakaTest;

class IllegalCallToNonWhiteListedMethod14 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(_1_000_000_000);
	}

	@Test @DisplayName("new Random()")
	void testNonWhiteListedCall() {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(), new ConstructorSignature(Random.class.getName()))
		);
	}
}