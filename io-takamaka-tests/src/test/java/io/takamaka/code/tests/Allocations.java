/**
 * 
 */
package io.takamaka.code.tests;

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
import io.hotmoka.beans.types.ClassType;

/**
 * A test for the remote purchase contract.
 */
class Allocations extends TakamakaTest {
	private static final BigInteger _20_000_000 = BigInteger.valueOf(20_000_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("allocations.jar", _1_000_000_000, BigInteger.valueOf(100_000L));
	}

	@Test @DisplayName("new Allocations()")
	void createAllocations() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(privateKey(0), account(0), _20_000_000, BigInteger.ONE, jar(), new ConstructorSignature(new ClassType("io.takamaka.tests.allocations.Allocations")));
	}
}