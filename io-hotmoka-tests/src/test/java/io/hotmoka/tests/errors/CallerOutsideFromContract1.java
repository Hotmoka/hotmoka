/**
 * 
 */
package io.hotmoka.tests.errors;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.tests.TakamakaTest;

class CallerOutsideFromContract1 extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() {
		throwsVerificationExceptionWithMessageContaining("caller() can only be used inside a @FromContract",
			() -> addJarStoreTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, takamakaCode(), bytesOf("calleroutsidefromcontract1.jar"), takamakaCode()));
	}
}