package io.hotmoka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;

/**
 * A test for an externally owned account with an enum field.
 */
class AccountWithEnum extends TakamakaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("accountwithenum.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(BigInteger.valueOf(1_000_000L));
	}

	@Test @DisplayName("creates account, funds it and checks that its ordinal() == 0")
	void callOrdinal() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());

		StorageReference account = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(),
			new ConstructorSignature("io.hotmoka.examples.accountwithenum.AccountWithEnum", ClassType.STRING), new StringValue(publicKey));

		addInstanceMethodCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(),
			MethodSignature.RECEIVE_INT, account, new IntValue(100_000));

		IntValue result = (IntValue) addInstanceMethodCallTransaction(keys.getPrivate(), account, _100_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature("io.hotmoka.examples.accountwithenum.AccountWithEnum", "ordinal", BasicTypes.INT), account);

		assertEquals(0, result.value);
	}
}