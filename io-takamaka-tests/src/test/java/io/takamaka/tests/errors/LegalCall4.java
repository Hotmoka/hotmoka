package io.takamaka.tests.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.takamaka.tests.TakamakaTest;

class LegalCall4 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);
	private static final ClassType C = new ClassType("io.takamaka.tests.errors.legalcall4.C");

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(_1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, CodeExecutionException, IOException, TransactionRejectedException {
		addJarStoreTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("legalcall4.jar"), takamakaCode());
	}

	@Test @DisplayName("new C().test(); toString() == \"33531\"")
	void newTestToString() throws TransactionException, CodeExecutionException, IOException, TransactionRejectedException {
		TransactionReference classpath = addJarStoreTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("legalcall4.jar"), takamakaCode());
		StorageReference c = addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature(C));
		postInstanceMethodCallTransaction(account(0), _20_000, BigInteger.ONE, classpath, new VoidMethodSignature(C, "test"), c);
		StringValue result = (StringValue) addInstanceMethodCallTransaction(account(0), _20_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(C, "toString", ClassType.STRING), c);

		assertEquals("33531", result.value);
	}
}