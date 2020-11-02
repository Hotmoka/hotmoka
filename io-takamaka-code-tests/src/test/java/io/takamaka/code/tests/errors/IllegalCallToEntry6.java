/**
 * 
 */
package io.takamaka.code.tests.errors;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.takamaka.code.tests.TakamakaTest;

class IllegalCallToEntry6 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(_1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException {
		throwsVerificationExceptionWithMessageContaining("is an @Entry, hence can only be called from an instance method or constructor of a contract", () -> 
			addJarStoreTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("illegalcalltoentry6.jar"), takamakaCode()));
	}
}