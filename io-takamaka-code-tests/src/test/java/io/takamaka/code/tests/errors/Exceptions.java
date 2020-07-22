package io.takamaka.code.tests.errors;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
import io.hotmoka.beans.values.NullValue;
import io.takamaka.code.tests.TakamakaTest;

class Exceptions extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(_1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, IOException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		addJarStoreTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("exceptions.jar"), takamakaCode());
	}

	@Test @DisplayName("install jar then calls foo1() and fails without program line")
	void callFoo1() throws TransactionException, IOException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		TransactionReference exceptions = addJarStoreTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("exceptions.jar"), takamakaCode());

		try {
			addStaticMethodCallTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, exceptions, new VoidMethodSignature("io.takamaka.tests.errors.exceptions.C", "foo1"));
		}
		catch (Exception e) {
			assertTrue(e instanceof TransactionException);
			assertTrue(e.getMessage().equals(NullPointerException.class.getName() + "@C.java:9"));
		}
	}

	@Test @DisplayName("install jar then calls foo2() and fails without program line")
	void callFoo2() throws TransactionException, IOException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		TransactionReference exceptions = addJarStoreTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("exceptions.jar"), takamakaCode());

		try {
			addStaticMethodCallTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, exceptions, new VoidMethodSignature("io.takamaka.tests.errors.exceptions.C", "foo2", ClassType.OBJECT), NullValue.INSTANCE);
		}
		catch (Exception e) {
			assertTrue(e instanceof TransactionException);
			assertTrue(e.getMessage().equals(NullPointerException.class.getName() + "@C.java:14"));
		}
	}
}