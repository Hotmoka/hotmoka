/**
 * 
 */
package io.takamaka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.EnumValue;
import io.hotmoka.nodes.DeserializationError;
import io.takamaka.tests.TakamakaTest;

class IllegalTypeForStorageField2 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("illegaltypeforstoragefield2.jar", _1_000_000_000);
	}

	@Test @DisplayName("store mutable enum into Object")
	void installJar() throws TransactionException, CodeExecutionException, IOException {
		throwsTransactionExceptionWithCause(DeserializationError.class, () ->
			addConstructorCallTransaction
				(privateKey(0), account(0), _20_000, BigInteger.ONE, jar(),
				new ConstructorSignature("io.takamaka.tests.errors.illegaltypeforstoragefield2.C", ClassType.OBJECT),
				new EnumValue("io.takamaka.tests.errors.illegaltypeforstoragefield2.MyEnum", "FIRST"))
		);
	}
}