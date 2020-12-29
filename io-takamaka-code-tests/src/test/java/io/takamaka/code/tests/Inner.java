/**
 * 
 */
package io.takamaka.code.tests;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

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
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for inner classes.
 */
class Inner extends TakamakaTest {
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final ConstructorSignature TEST_INNER_CONSTRUCTOR = new ConstructorSignature("io.takamaka.tests.inner.TestInner");

	// do not forget the implicit parameter holding the parent of the inner object
	private static final ConstructorSignature TEST_INNER_INSIDE_CONSTRUCTOR = new ConstructorSignature("io.takamaka.tests.inner.TestInner$Inside",
		new ClassType("io.takamaka.tests.inner.TestInner"), BasicTypes.LONG);

	private static final NonVoidMethodSignature TEST_INNER_INSIDE_GETBALANCE = new NonVoidMethodSignature("io.takamaka.tests.inner.TestInner$Inside", "getBalance", ClassType.BIG_INTEGER);

	private static final NonVoidMethodSignature TEST_INNER_INSIDE_GETPARENT = new NonVoidMethodSignature("io.takamaka.tests.inner.TestInner$Inside", "getParent",
		new ClassType("io.takamaka.tests.inner.TestInner"));

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("inner.jar", BigInteger.valueOf(100_000L));
	}

	@Test @DisplayName("new TestInner()")
	void newTestInner() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(privateKey(0), account(0), _10_000, ONE, jar(), TEST_INNER_CONSTRUCTOR);
	}

	@Test @DisplayName("(new TestInner().new Inside(1000)).getBalance() == 1000")
	void newTestInnerInsideGetBalance() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference testInner = addConstructorCallTransaction(privateKey(0), account(0), _10_000, ONE, jar(), TEST_INNER_CONSTRUCTOR);
		StorageReference inside = addConstructorCallTransaction(privateKey(0), account(0), _10_000, ONE, jar(), TEST_INNER_INSIDE_CONSTRUCTOR, testInner, new LongValue(1000L));
		BigIntegerValue balance = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _10_000, jar(), TEST_INNER_INSIDE_GETBALANCE, inside);
		
		assertEquals(balance.value, BigInteger.valueOf(1000L));
	}

	@Test @DisplayName("ti = new TestInner(); (ti.new Inside(1000)).getParent() == ti")
	void newTestInnerInsideGetParent() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference testInner = addConstructorCallTransaction(privateKey(0), account(0), _10_000, ONE, jar(), TEST_INNER_CONSTRUCTOR);
		StorageReference inside = addConstructorCallTransaction(privateKey(0), account(0), _10_000, ONE, jar(), TEST_INNER_INSIDE_CONSTRUCTOR, testInner, new LongValue(1000L));
		StorageReference parent = (StorageReference) runInstanceMethodCallTransaction(account(0), _10_000, jar(), TEST_INNER_INSIDE_GETPARENT, inside);
		
		assertEquals(testInner, parent);
	}
}