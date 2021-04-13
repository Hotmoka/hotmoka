package io.hotmoka.tests;

import static java.math.BigInteger.ONE;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;

/**
 * A test that split packages are not allowed.
 */
class SplitPackage extends TakamakaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("basicdependency.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, BigInteger.ZERO);
	}

	@Test @DisplayName("jars with distinct packages coexist")
	void testDisjointJars() throws TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException, IOException {
		addJarStoreTransaction(privateKey(0), account(0), _1_000_000, ONE, takamakaCode(), bytesOf("basic.jar"), jar());
	}
	
	@Test @DisplayName("jars with packages split among them cannot be put together")
	void testSplitPackages() {
		throwsTransactionRejectedWithCause(IllegalArgumentException.class, () ->
			addJarStoreTransaction(privateKey(0), account(0), _1_000_000, ONE, takamakaCode(), bytesOf("basicdependency.jar"), jar()));
	}
}