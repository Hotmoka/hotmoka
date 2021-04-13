package io.hotmoka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.tests.TakamakaTest;

class LegalCall5 extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, IOException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("legalcall5.jar"), takamakaCode());
	}

	@Test @DisplayName("new C().foo()")
	void newTestToString() throws TransactionException, CodeExecutionException, IOException, TransactionRejectedException, InvalidKeyException, SignatureException {
		TransactionReference jar = addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("legalcall5.jar"), takamakaCode());

		addStaticMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar, new VoidMethodSignature(new ClassType("io.hotmoka.examples.errors.legalcall5.C"), "foo"));
	}
}