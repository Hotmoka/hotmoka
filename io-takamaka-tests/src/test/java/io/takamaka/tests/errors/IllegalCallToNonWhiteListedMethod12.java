package io.takamaka.tests.errors;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.takamaka.tests.TakamakaTest;

class IllegalCallToNonWhiteListedMethod12 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		mkMemoryBlockchain(_1_000_000_000);
	}

	@Test @DisplayName("new ExternallyOwnedAccount().hashCode()")
	void testNonWhiteListedCall() throws TransactionException, CodeExecutionException {
		StorageReference eoa = addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(), new ConstructorSignature(ClassType.EOA));

		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			addInstanceMethodCallTransaction(account(0), _20_000, BigInteger.ONE, takamakaCode(), new NonVoidMethodSignature(ClassType.OBJECT, "hashCode", BasicTypes.INT), eoa)
		);
	}
}