package io.hotmoka.tests;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test of the @SelfCharged annotation.
 */
class SelfCharged extends TakamakaTest {
	private final static BigInteger _1_000_000 = BigInteger.valueOf(1_000_000);
	private final static BigInteger _10_000 = BigInteger.valueOf(10_000);
	private final static ClassType SELF_CHARGEABLE = new ClassType("io.takamaka.tests.selfcharged.SelfChargeable");

	@BeforeEach
	void beforeEach() throws Exception {
		if (consensus != null && consensus.allowsSelfCharged)
			setNode("selfcharged.jar", _1_000_000, ZERO);
	}

	@Test @DisplayName("new C(100_000).foo() fails when called by an account with zero balance")
	void failsForNonSelfCharged() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		if (consensus != null && consensus.allowsSelfCharged) {
			StorageReference sc = addConstructorCallTransaction(privateKey(0), account(0), _10_000, ONE, jar(), new ConstructorSignature(SELF_CHARGEABLE, BasicTypes.INT), new IntValue(100_000));
			try {
				addInstanceMethodCallTransaction(privateKey(1), account(1), _10_000, ONE, jar(), new VoidMethodSignature(SELF_CHARGEABLE, "foo"), sc);
			}
			catch (TransactionRejectedException e) {
				assertEquals("the payer has not enough funds to buy 10000 units of gas", e.getMessage());
				return;
			}

			fail();
		}
	}

	@Test @DisplayName("new C(100_000).goo() succeeds when called by an account with zero balance")
	void succeedsForSelfCharged() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		if (consensus != null && consensus.allowsSelfCharged) {
			StorageReference sc = addConstructorCallTransaction(privateKey(0), account(0), _10_000, ONE, jar(), new ConstructorSignature(SELF_CHARGEABLE, BasicTypes.INT), new IntValue(100_000));
			addInstanceMethodCallTransaction(privateKey(1), account(1), _10_000, ONE, jar(), new VoidMethodSignature(SELF_CHARGEABLE, "goo"), sc);
		}
	}
}