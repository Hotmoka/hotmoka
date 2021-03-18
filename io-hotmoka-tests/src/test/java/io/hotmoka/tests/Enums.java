/**
 * 
 */
package io.hotmoka.tests;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.EnumValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for the use of enumeration types.
 */
class Enums extends TakamakaTest {
	private static final ClassType MY_ENUM = new ClassType("io.hotmoka.examples.enums.MyEnum");

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("enums.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, _100_000);
	}

	@Test @DisplayName("new TestEnums(MyEnum.PRESENT)")
	void testEnumAsActual() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(privateKey(0), account(0), _10_000_000, BigInteger.ONE, jar(),
			new ConstructorSignature("io.hotmoka.examples.enums.TestEnums", MY_ENUM), new EnumValue("io.hotmoka.examples.enums.MyEnum", "PRESENT"));
	}

	@Test @DisplayName("new TestEnums(MyEnum.PRESENT).getOrdinal() == 1")
	void testGetOrdinal() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference testEnums = addConstructorCallTransaction(privateKey(0), account(0), _10_000_000, ONE, jar(),
			new ConstructorSignature("io.hotmoka.examples.enums.TestEnums", MY_ENUM), new EnumValue("io.hotmoka.examples.enums.MyEnum", "PRESENT"));

		IntValue ordinal = (IntValue) runInstanceMethodCallTransaction(account(0), _10_000, jar(),
			new NonVoidMethodSignature("io.hotmoka.examples.enums.TestEnums", "getOrdinal", BasicTypes.INT), testEnums);

		assertSame(1, ordinal.value);
	}

	@Test @DisplayName("TestEnums.getFor(2) == MyEnum.FUTURE")
	void testGetFor() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		EnumValue element = (EnumValue) runStaticMethodCallTransaction(account(0), _10_000, jar(),
			new NonVoidMethodSignature("io.hotmoka.examples.enums.TestEnums", "getFor", MY_ENUM, BasicTypes.INT), new IntValue(2));

		assertEquals(new EnumValue(MY_ENUM.name, "FUTURE"), element);
	}
}