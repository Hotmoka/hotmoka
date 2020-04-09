package io.takamaka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.takamaka.tests.TakamakaTest;

class LegalCall5 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		mkBlockchain(_1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, CodeExecutionException, IOException, TransactionRejectedException {
		addJarStoreTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("legalcall5.jar"), takamakaCode());
	}

	@Test @DisplayName("new C().foo()")
	void newTestToString() throws TransactionException, CodeExecutionException, IOException, TransactionRejectedException {
		TransactionReference jar = addJarStoreTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("legalcall5.jar"), takamakaCode());

		addStaticMethodCallTransaction(account(0), _20_000, BigInteger.ONE, new Classpath(jar, true), new VoidMethodSignature(new ClassType("io.takamaka.tests.errors.legalcall5.C"), "foo"));
	}
}