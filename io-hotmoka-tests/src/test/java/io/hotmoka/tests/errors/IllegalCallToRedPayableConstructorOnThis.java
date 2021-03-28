/**
 * 
 */
package io.hotmoka.tests.errors;

import static java.math.BigInteger.ONE;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.tests.TakamakaTest;

class IllegalCallToRedPayableConstructorOnThis extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException {
		throwsVerificationExceptionWithMessageContaining("only a @RedPayable constructor can call another @RedPayable constructor by chaining", () ->
			addJarStoreTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), bytesOf("illegalcallonthis2.jar"), takamakaCode())
		);
	}
}