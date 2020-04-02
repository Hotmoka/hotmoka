/**
 * 
 */
package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for the remote purchase contract.
 */
class AbstractFail extends TakamakaTest {

	private static final ClassType ABSTRACT_FAIL = new ClassType("io.takamaka.tests.abstractfail.AbstractFail");
	private static final ClassType ABSTRACT_FAIL_IMPL = new ClassType("io.takamaka.tests.abstractfail.AbstractFailImpl");

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		mkMemoryBlockchain(_1_000_000_000, BigInteger.valueOf(100_000L), BigInteger.valueOf(1_000_000L));

		TransactionReference abstractfail = addJarStoreTransaction
			(account(0), _20_000, BigInteger.ONE, takamakaCode(), bytesOf("abstractfail.jar"), takamakaCode());

		classpath = new Classpath(abstractfail, true);
	}

	@Test @DisplayName("new AbstractFail() throws InstantiationException")
	void createAbstractFail() throws TransactionException, CodeExecutionException {
		throwsTransactionExceptionWithCause(InstantiationException.class, () ->
			// cannot instantiate an abstract class
			addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature(ABSTRACT_FAIL))
		);
	}

	@Test @DisplayName("new AbstractFailImpl()")
	void createAbstractFailImpl() throws TransactionException, CodeExecutionException {
		addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, classpath,
			new ConstructorSignature(ABSTRACT_FAIL_IMPL, BasicTypes.INT), new IntValue(42));
	}

	@Test @DisplayName("new AbstractFailImpl().method() yields an AbstractFailImpl")
	void createAbstractFailImplThenCallAbstractMethod() throws TransactionException, CodeExecutionException {
		StorageReference abstractfail = addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, classpath,
			new ConstructorSignature(ABSTRACT_FAIL_IMPL, BasicTypes.INT), new IntValue(42));

		StorageReference result = (StorageReference) addInstanceMethodCallTransaction
			(account(0), _20_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(ABSTRACT_FAIL, "method", ABSTRACT_FAIL),
			abstractfail);

		assertEquals("io.takamaka.tests.abstractfail.AbstractFailImpl", getClassNameOf(result));
	}
}