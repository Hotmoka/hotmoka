package io.takamaka.tests.errors;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BooleanValue;
import io.takamaka.tests.TakamakaTest;

class LegalCall3 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		mkBlockchain(_1_000_000_000);
	}

	@Test @DisplayName("C.test() == false")
	void callTest() throws TransactionException, CodeExecutionException, IOException {
		TransactionReference jar = addJarStoreTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("legalcall3.jar"), takamakaCode());

		BooleanValue result = (BooleanValue) addStaticMethodCallTransaction(account(0), _20_000, BigInteger.ONE, new Classpath(jar, true),
			new NonVoidMethodSignature(new ClassType("io.takamaka.tests.errors.legalcall3.C"), "test", BasicTypes.BOOLEAN));

		assertFalse(result.value);
	}
}