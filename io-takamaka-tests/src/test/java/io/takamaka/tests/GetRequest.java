/**
 * 
 */
package io.takamaka.tests;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for {@linkplain #AbstractNode#getRequestAt(io.hotmoka.beans.references.TransactionReference)}}.
 */
class GetRequest extends TakamakaTest {
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);
	private static final ConstructorSignature ABSTRACT_FAIL_IMPL_CONSTRUCTOR = new ConstructorSignature(new ClassType("io.takamaka.tests.abstractfail.AbstractFailImpl"), BasicTypes.INT);
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("abstractfail.jar", ALL_FUNDS);
	}

	@Test @DisplayName("getRequestAt works")
	void entryFilterFailsWithoutThrowsExceptions() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, new IntValue(42));
		TransactionRequest<?> request = getRequestAt(abstractfail.transaction);
		Assertions.assertTrue(request instanceof ConstructorCallTransactionRequest);
		Assertions.assertEquals(account(0), ((ConstructorCallTransactionRequest) request).caller);
	}
}