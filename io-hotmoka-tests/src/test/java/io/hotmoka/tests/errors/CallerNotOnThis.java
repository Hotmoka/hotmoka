package io.hotmoka.tests.errors;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.tests.TakamakaTest;

class CallerNotOnThis extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() {
		throwsVerificationExceptionWithMessageContaining("caller() can only be called on \"this\"", () ->
			addJarStoreTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, takamakaCode(), bytesOf("callernotonthis.jar"), takamakaCode()));
	}
}