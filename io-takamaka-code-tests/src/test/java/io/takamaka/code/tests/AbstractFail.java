/**
 * 
 */
package io.takamaka.code.tests;

import static io.hotmoka.beans.Coin.panarea;
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
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.takamaka.code.constants.Constants;

/**
 * A test for the remote purchase contract.
 */
class AbstractFail extends TakamakaTest {
	private static final ClassType ABSTRACT_FAIL = new ClassType("io.hotmoka.tests.abstractfail.AbstractFail");
	private static final ConstructorSignature ABSTRACT_FAIL_IMPL_CONSTRUCTOR = new ConstructorSignature(new ClassType("io.hotmoka.tests.abstractfail.AbstractFailImpl"), BasicTypes.INT);
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("abstractfail.jar", _1_000_000_000, BigInteger.valueOf(100_000L), BigInteger.valueOf(1_000_000L));
	}

	@Test @DisplayName("new AbstractFail() throws InstantiationException")
	void createAbstractFail() throws TransactionException, CodeExecutionException {
		throwsTransactionExceptionWithCause(InstantiationException.class, () ->
			// cannot instantiate an abstract class
			addConstructorCallTransaction(privateKey(0), account(0), _20_000, panarea(1), jar(), new ConstructorSignature(ABSTRACT_FAIL))
		);
	}

	@Test @DisplayName("new AbstractFailImpl()")
	void createAbstractFailImpl() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(privateKey(0), account(0), _20_000, panarea(1), jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, new IntValue(42));
	}

	@Test @DisplayName("new AbstractFailImpl().method() yields an AbstractFailImpl")
	void createAbstractFailImplThenCallAbstractMethod() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _20_000, panarea(1), jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, new IntValue(42));

		StorageReference result = (StorageReference) addInstanceMethodCallTransaction
			(privateKey(0), account(0), _20_000, panarea(1), jar(), new NonVoidMethodSignature(ABSTRACT_FAIL, "method", ABSTRACT_FAIL), abstractfail);

		String className = ((StringValue) runInstanceMethodCallTransaction(account(0), _20_000, jar(), new NonVoidMethodSignature(Constants.STORAGE_NAME, "getClassName", ClassType.STRING), result)).value;

		assertEquals("io.hotmoka.tests.abstractfail.AbstractFailImpl", className);
	}
}