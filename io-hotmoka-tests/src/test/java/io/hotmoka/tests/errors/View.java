/**
 * 
 */
package io.hotmoka.tests.errors;

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
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.SideEffectsInViewMethodException;
import io.hotmoka.tests.TakamakaTest;

class View extends TakamakaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("view.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("install jar then call to View.no1() fails")
	void callNo1() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, jar(), new ConstructorSignature("io.hotmoka.tests.errors.view.C"));

		TakamakaTest.throwsTransactionExceptionWithCause(NoSuchMethodException.class, () -> 
			runInstanceMethodCallTransaction(account(0), _20_000, jar(),
				new NonVoidMethodSignature("io.hotmoka.tests.errors.view.C", "no1", BasicTypes.INT, BasicTypes.INT, BasicTypes.INT),
				c, new IntValue(13), new IntValue(17)));
	}

	@Test @DisplayName("install jar then call to View.no2() fails")
	void callNo2() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, jar(), new ConstructorSignature("io.hotmoka.tests.errors.view.C"));

		TakamakaTest.throwsTransactionExceptionWithCause(SideEffectsInViewMethodException.class, () -> 
			runInstanceMethodCallTransaction(account(0), _20_000, jar(),
				new NonVoidMethodSignature("io.hotmoka.tests.errors.view.C", "no2", BasicTypes.INT, BasicTypes.INT, BasicTypes.INT),
				c, new IntValue(13), new IntValue(17)));
	}

	@Test @DisplayName("install jar then call to View.yes() succeeds")
	void callYes() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _20_000, BigInteger.ONE, jar(), new ConstructorSignature("io.hotmoka.tests.errors.view.C"));

		runInstanceMethodCallTransaction(account(0), _20_000, jar(),
			new NonVoidMethodSignature("io.hotmoka.tests.errors.view.C", "yes", BasicTypes.INT, BasicTypes.INT, BasicTypes.INT),
			c, new IntValue(13), new IntValue(17));
	}
}