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

package io.hotmoka.tests;

import static io.hotmoka.helpers.Coin.panarea;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.UnmatchedTargetException;

/**
 * A test for the remote purchase contract.
 */
class AbstractFail extends HotmokaTest {
	private static final ClassType ABSTRACT_FAIL = StorageTypes.classNamed("io.hotmoka.examples.abstractfail.AbstractFail");
	private static final ConstructorSignature ABSTRACT_FAIL_IMPL_CONSTRUCTOR = ConstructorSignatures.of
		(StorageTypes.classNamed("io.hotmoka.examples.abstractfail.AbstractFailImpl"),
		StorageTypes.INT);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("abstractfail.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, _100_000, _1_000_000);
	}

	@Test @DisplayName("new AbstractFail() throws UnmatchedTargetException")
	void createAbstractFail() {
		throwsTransactionExceptionWithCause(UnmatchedTargetException.class, () ->
			// cannot instantiate an abstract class
			addConstructorCallTransaction(privateKey(0), account(0), _100_000, panarea(1), jar(), ConstructorSignatures.of(ABSTRACT_FAIL))
		);
	}

	@Test @DisplayName("new AbstractFailImpl()")
	void createAbstractFailImpl() throws Exception {
		addConstructorCallTransaction(privateKey(0), account(0), _100_000, panarea(1), jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, StorageValues.intOf(42));
	}

	@Test @DisplayName("new AbstractFailImpl().method() yields an AbstractFailImpl")
	void createAbstractFailImplThenCallAbstractMethod() throws Exception {
		StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _100_000, panarea(1), jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, StorageValues.intOf(42));

		var abstractFailMethod = MethodSignatures.ofNonVoid(ABSTRACT_FAIL, "method", ABSTRACT_FAIL);
		StorageReference result = addInstanceNonVoidMethodCallTransaction
			(privateKey(0), account(0), _100_000, panarea(1), jar(), abstractFailMethod, abstractfail).asReturnedReference(abstractFailMethod, UnexpectedValueException::new);

		var getClassName = MethodSignatures.ofNonVoid(StorageTypes.STORAGE, "getClassName", StorageTypes.STRING);
		String className = runInstanceNonVoidMethodCallTransaction(account(0), _100_000, jar(), getClassName, result).asReturnedString(getClassName, UnexpectedValueException::new);

		assertEquals("io.hotmoka.examples.abstractfail.AbstractFailImpl", className);
	}
}