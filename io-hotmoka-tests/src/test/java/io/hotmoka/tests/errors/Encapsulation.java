/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.tests.errors;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.updates.UpdateOfField;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.constants.Constants;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.tests.HotmokaTest;

class Encapsulation extends HotmokaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("encapsulation.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar then finds out the reference of list1, calls clear() on it and then size1() == 0")
	void modifiesList1() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference encapsulated = addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, jar(),
				ConstructorSignatures.of("io.hotmoka.examples.errors.encapsulation.Encapsulated"));

		// we determine the storage reference of list1
		StorageReference list1 = (StorageReference) node.getState(encapsulated)
			.filter(update -> update instanceof UpdateOfField)
			.map(update -> (UpdateOfField) update)
			.filter(update -> "list1".equals(update.getField().getName()))
			.map(UpdateOfField::getValue)
			.findFirst()
			.get();

		// we call clear() on list1, directly! This works since list1 is exported
		addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(),
				MethodSignatures.ofVoid(Constants.STORAGE_LIST_NAME, "clear"),
			list1);

		IntValue result = (IntValue) runInstanceMethodCallTransaction(account(0), _100_000, jar(),
			MethodSignatures.of("io.hotmoka.examples.errors.encapsulation.Encapsulated", "size1", StorageTypes.INT),
			encapsulated);

		assertSame(0, result.getValue());
	}

	@Test @DisplayName("install jar then finds out the reference of list2, calls clear() on it and it fails")
	void modifiesList2Fails() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference encapsulated = addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, jar(),
				ConstructorSignatures.of("io.hotmoka.examples.errors.encapsulation.Encapsulated"));

		// we determine the storage reference of list2
		StorageReference list2 = (StorageReference) node.getState(encapsulated)
			.filter(update -> update instanceof UpdateOfField)
			.map(update -> (UpdateOfField) update)
			.filter(update -> "list2".equals(update.getField().getName()))
			.map(UpdateOfField::getValue)
			.findFirst()
			.get();

		// we call clear() on list2, directly! This will fail since list2 is not exported
		throwsTransactionRejectedWithCause("cannot pass as argument a value of the non-exported type io.takamaka.code.util.",
			() -> addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(),
			MethodSignatures.ofVoid(Constants.STORAGE_LIST_NAME, "clear"),
			list2));
	}
}