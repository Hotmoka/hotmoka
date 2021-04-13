package io.hotmoka.tests.errors;

import io.hotmoka.tests.TakamakaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

class IllegalCallToFromContract7 extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() {
		throwsVerificationExceptionWithMessageContaining("is @FromContract, hence can only be called from an instance method or constructor of a contract", () -> 
			addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("illegalcalltofromcontract7.jar"), takamakaCode()));
	}
}