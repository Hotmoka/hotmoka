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

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.SideEffectsInViewMethodException;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.tests.HotmokaTest;

class View extends HotmokaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("view.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar then call to View.no1() fails")
	void callNo1() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), ConstructorSignatures.of("io.hotmoka.examples.errors.view.C"));

		throwsTransactionExceptionWithCause(NoSuchMethodException.class, () -> 
			runInstanceMethodCallTransaction(account(0), _100_000, jar(),
				MethodSignatures.ofNonVoid("io.hotmoka.examples.errors.view.C", "no1", StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
				c, StorageValues.intOf(13), StorageValues.intOf(17)));
	}

	@Test @DisplayName("install jar then call to View.no2() fails")
	void callNo2() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), ConstructorSignatures.of("io.hotmoka.examples.errors.view.C"));

		throwsTransactionExceptionWithCause(SideEffectsInViewMethodException.class, () -> 
			runInstanceMethodCallTransaction(account(0), _100_000, jar(),
				MethodSignatures.ofNonVoid("io.hotmoka.examples.errors.view.C", "no2", StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
				c, StorageValues.intOf(13), StorageValues.intOf(17)));
	}

	@Test @DisplayName("install jar then call to View.yes() succeeds")
	void callYes() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), ConstructorSignatures.of("io.hotmoka.examples.errors.view.C"));

		runInstanceMethodCallTransaction(account(0), _100_000, jar(),
			MethodSignatures.ofNonVoid("io.hotmoka.examples.errors.view.C", "yes", StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
			c, StorageValues.intOf(13), StorageValues.intOf(17));
	}
}