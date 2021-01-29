package io.hotmoka.tests.errors;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.tests.TakamakaTest;
import io.takamaka.code.constants.Constants;

class Encapsulation extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("encapsulation.jar", _1_000_000_000);
	}

	@Test @DisplayName("install jar then finds out the reference of list1, calls clear() on it and then size1() == 0")
	void modifiesList1() throws TransactionException, IOException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		StorageReference encapsulated = addConstructorCallTransaction(privateKey(0), account(0), _20_000, ONE, jar(),
			new ConstructorSignature("io.hotmoka.tests.errors.encapsulation.Encapsulated"));

		// we determine the storage reference of list1
		StorageReference list1 = (StorageReference) originalView.getState(encapsulated)
			.filter(update -> update instanceof UpdateOfField)
			.map(update -> (UpdateOfField) update)
			.filter(update -> "list1".equals(update.getField().name))
			.map(update -> update.getValue())
			.findFirst()
			.get();

		// we call clear() on list1, directly! This works since list1 is exported
		addInstanceMethodCallTransaction(privateKey(0), account(0), _20_000, ONE, jar(),
			new VoidMethodSignature(Constants.STORAGE_LIST_NAME, "clear"),
			list1);

		IntValue result = (IntValue) runInstanceMethodCallTransaction(account(0), _20_000, jar(),
			new NonVoidMethodSignature("io.hotmoka.tests.errors.encapsulation.Encapsulated", "size1", BasicTypes.INT),
			encapsulated);

		assertSame(0, result.value);
	}

	@Test @DisplayName("install jar then finds out the reference of list2, calls clear() on it and it fails")
	void modifiesList2Fails() throws TransactionException, IOException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		StorageReference encapsulated = addConstructorCallTransaction(privateKey(0), account(0), _20_000, ONE, jar(),
			new ConstructorSignature("io.hotmoka.tests.errors.encapsulation.Encapsulated"));

		// we determine the storage reference of list2
		StorageReference list2 = (StorageReference) originalView.getState(encapsulated)
			.filter(update -> update instanceof UpdateOfField)
			.map(update -> (UpdateOfField) update)
			.filter(update -> "list2".equals(update.getField().name))
			.map(update -> update.getValue())
			.findFirst()
			.get();

		// we call clear() on list2, directly! This will fail since list2 is not exported
		throwsTransactionRejectedWithCause("cannot pass as argument a value of the non-exported type io.takamaka.code.util.",
			() -> addInstanceMethodCallTransaction(privateKey(0), account(0), _20_000, ONE, jar(),
			new VoidMethodSignature(Constants.STORAGE_LIST_NAME, "clear"),
			list2));
	}
}