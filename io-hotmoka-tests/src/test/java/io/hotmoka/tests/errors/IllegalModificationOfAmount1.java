package io.hotmoka.tests.errors;

import static java.math.BigInteger.ONE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.tests.TakamakaTest;

class IllegalModificationOfAmount1 extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() {
		throwsVerificationExceptionWithMessageContaining("the paid amount cannot be changed in constructor chaining", () ->
			addJarStoreTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), bytesOf("illegalmodificationofamount1.jar"), takamakaCode())
		);
	}
}