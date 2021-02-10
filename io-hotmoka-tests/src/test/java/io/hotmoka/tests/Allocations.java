/**
 * 
 */
package io.hotmoka.tests;

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
import io.hotmoka.beans.types.ClassType;

/**
 * A test for the memory allocation bytecodes.
 */
class Allocations extends TakamakaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("allocations.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, _100_000);
	}

	@Test @DisplayName("new Allocations()")
	void createAllocations() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(privateKey(0), account(0), _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(new ClassType("io.hotmoka.examples.allocations.Allocations")));
	}
}