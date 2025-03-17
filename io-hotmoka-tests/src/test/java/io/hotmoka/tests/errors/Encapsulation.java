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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.tests.HotmokaTest;
import io.takamaka.code.constants.Constants;

class Encapsulation extends HotmokaTest {

	private final static ClassType ENCAPSULATED = StorageTypes.classNamed("io.hotmoka.examples.errors.encapsulation.Encapsulated");

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("encapsulation.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar then finds out the reference of list1, calls clear() on it and then size1() == 0")
	void modifiesList1() throws Exception {
		StorageReference encapsulated = addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, jar(), ConstructorSignatures.of(ENCAPSULATED));

		// we determine the storage reference of list1
		var list1 = (StorageReference) node.getState(encapsulated)
			.filter(update -> update instanceof UpdateOfField)
			.map(update -> (UpdateOfField) update)
			.filter(update -> "list1".equals(update.getField().getName()))
			.map(UpdateOfField::getValue)
			.findFirst()
			.get();

		// we call clear() on list1, directly! This works since list1 is exported
		addInstanceVoidMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(),
				MethodSignatures.ofVoid(StorageTypes.classNamed(Constants.STORAGE_LIST_NAME), "clear"),
			list1);

		var result = (IntValue) runInstanceNonVoidMethodCallTransaction(account(0), _100_000, jar(),
			MethodSignatures.ofNonVoid(ENCAPSULATED, "size1", StorageTypes.INT),
			encapsulated);

		assertSame(0, result.getValue());
	}

	@Test @DisplayName("install jar then finds out the reference of list2, calls clear() on it and it fails")
	void modifiesList2Fails() throws Exception {
		StorageReference encapsulated = addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, jar(),
				ConstructorSignatures.of(ENCAPSULATED));

		// we determine the storage reference of list2
		var list2 = (StorageReference) node.getState(encapsulated)
			.filter(update -> update instanceof UpdateOfField)
			.map(update -> (UpdateOfField) update)
			.filter(update -> "list2".equals(update.getField().getName()))
			.map(UpdateOfField::getValue)
			.findFirst()
			.get();

		// we call clear() on list2, directly! This will fail since list2 is not exported
		TransactionRejectedException e = assertThrows(TransactionRejectedException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(),
				MethodSignatures.ofVoid(StorageTypes.classNamed(Constants.STORAGE_LIST_NAME), "clear"), list2));
		assertTrue(e.getMessage().contains("is not exported"));
	}
}