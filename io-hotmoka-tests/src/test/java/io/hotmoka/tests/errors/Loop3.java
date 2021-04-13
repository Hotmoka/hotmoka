package io.hotmoka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.hotmoka.tests.TakamakaTest;

class Loop3 extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, IOException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("loop3.jar"), takamakaCode());
	}

	@Test @DisplayName("install jar then call to Loop.loop() fails")
	void callLoop() throws TransactionException, IOException, TransactionRejectedException, InvalidKeyException, SignatureException {
		TransactionReference loop = addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("loop3.jar"), takamakaCode());

		TakamakaTest.throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () -> 
			addStaticMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, loop, new VoidMethodSignature("io.hotmoka.examples.errors.loop3.Loop", "loop")));
	}
}