/**
 * 
 */
package io.takamaka.tests;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.ClassType;

/**
 * A test for the remote purchase contract.
 */
class Allocations extends TakamakaTest {

	private static final ClassType ALLOCATIONS = new ClassType("io.takamaka.tests.allocations.Allocations");

	private static final BigInteger _20_000_000 = BigInteger.valueOf(20_000_000);

	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		mkBlockchain(_1_000_000_000, BigInteger.valueOf(100_000L));

		TransactionReference allocations = addJarStoreTransaction
			(account(0), _20_000_000, BigInteger.ONE, takamakaCode(), bytesOf("allocations.jar"), takamakaCode());

		classpath = new Classpath(allocations, true);
	}

	@Test @DisplayName("new Allocations()")
	void createAllocations() throws TransactionException, CodeExecutionException {
		addConstructorCallTransaction(account(0), _20_000_000, BigInteger.ONE, classpath, new ConstructorSignature(ALLOCATIONS));
	}
}