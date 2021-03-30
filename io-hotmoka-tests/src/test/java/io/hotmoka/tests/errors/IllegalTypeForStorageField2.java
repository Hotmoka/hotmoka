/**
 * 
 */
package io.hotmoka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.EnumValue;
import io.hotmoka.nodes.DeserializationError;
import io.hotmoka.tests.TakamakaTest;

class IllegalTypeForStorageField2 extends TakamakaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("illegaltypeforstoragefield2.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("store mutable enum into Object")
	void installJar() throws TransactionException, CodeExecutionException, IOException {
		throwsTransactionExceptionWithCause(DeserializationError.class, () ->
			addConstructorCallTransaction
				(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(),
				new ConstructorSignature("io.takamaka.tests.errors.illegaltypeforstoragefield2.C", ClassType.OBJECT),
				new EnumValue("io.takamaka.tests.errors.illegaltypeforstoragefield2.MyEnum", "FIRST"))
		);
	}
}