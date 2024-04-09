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

import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.takamaka.code.constants.Constants;

/**
 * A test for the remote purchase contract.
 */
class AbstractFail extends HotmokaTest {
	private static final ClassType ABSTRACT_FAIL = StorageTypes.classNamed("io.hotmoka.examples.abstractfail.AbstractFail");
	private static final ConstructorSignature ABSTRACT_FAIL_IMPL_CONSTRUCTOR = ConstructorSignatures.of(StorageTypes.classNamed("io.hotmoka.examples.abstractfail.AbstractFailImpl"), StorageTypes.INT);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("abstractfail.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, _100_000, _1_000_000);
	}

	@Test @DisplayName("new AbstractFail() throws InstantiationException")
	void createAbstractFail() {
		throwsTransactionExceptionWithCause(InstantiationException.class, () ->
			// cannot instantiate an abstract class
			addConstructorCallTransaction(privateKey(0), account(0), _100_000, panarea(1), jar(), ConstructorSignatures.of(ABSTRACT_FAIL))
		);
	}

	@Test @DisplayName("new AbstractFailImpl()")
	void createAbstractFailImpl() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		addConstructorCallTransaction(privateKey(0), account(0), _100_000, panarea(1), jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, StorageValues.intOf(42));
	}

	@Test @DisplayName("new AbstractFailImpl().method() yields an AbstractFailImpl")
	void createAbstractFailImplThenCallAbstractMethod() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _100_000, panarea(1), jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, StorageValues.intOf(42));

		StorageReference result = (StorageReference) addInstanceMethodCallTransaction
			(privateKey(0), account(0), _100_000, panarea(1), jar(), MethodSignatures.of(ABSTRACT_FAIL, "method", ABSTRACT_FAIL), abstractfail);

		String className = ((StringValue) runInstanceMethodCallTransaction(account(0), _100_000, jar(), MethodSignatures.of(Constants.STORAGE_NAME, "getClassName", StorageTypes.STRING), result)).getValue();

		assertEquals("io.hotmoka.examples.abstractfail.AbstractFailImpl", className);
	}
}