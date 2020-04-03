/**
 * 
 */
package io.takamaka.tests.errors;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.takamaka.tests.TakamakaTest;

class IllegalCallToEntry1 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		mkBlockchain(_1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() {
		throwsVerificationException(() ->
			addJarStoreTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("illegalcalltoentry1.jar"), takamakaCode())
		);
	}
}