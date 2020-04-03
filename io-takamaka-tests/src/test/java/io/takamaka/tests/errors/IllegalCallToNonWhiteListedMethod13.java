package io.takamaka.tests.errors;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.takamaka.code.whitelisting.HasDeterministicTerminatingHashCode;
import io.takamaka.tests.TakamakaTest;

public class IllegalCallToNonWhiteListedMethod13 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		mkBlockchain(_1_000_000_000);
	}

	@Test @DisplayName("call with argument with non-deterministic hashCode()")
	void testNonWhiteListedCall() throws TransactionException, CodeExecutionException {
		StorageReference eoa = addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(), new ConstructorSignature(ClassType.EOA));

		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			addStaticMethodCallTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(),
				new VoidMethodSignature(IllegalCallToNonWhiteListedMethod13.class.getName(), "callee", ClassType.OBJECT),
				eoa)
		);
	}

	public static void callee(@HasDeterministicTerminatingHashCode Object o) {}
}