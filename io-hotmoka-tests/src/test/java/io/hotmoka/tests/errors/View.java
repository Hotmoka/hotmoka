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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.SideEffectsInViewMethodException;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.tests.HotmokaTest;

class View extends HotmokaTest {

	private final static ClassType C = StorageTypes.classNamed("io.hotmoka.examples.errors.view.C");

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("view.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar then call to View.no1() fails")
	void callNo1() throws Exception {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), ConstructorSignatures.of(C));

		throwsTransactionExceptionWithCause(NoSuchMethodException.class, () -> 
			runInstanceNonVoidMethodCallTransaction(account(0), _100_000, jar(),
				MethodSignatures.ofNonVoid(C, "no1", StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
				c, StorageValues.intOf(13), StorageValues.intOf(17)));
	}

	@Test @DisplayName("install jar then call to View.no2() fails")
	void callNo2() throws Exception {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), ConstructorSignatures.of(C));

		throwsTransactionExceptionWithCause(SideEffectsInViewMethodException.class, () -> 
			runInstanceNonVoidMethodCallTransaction(account(0), _100_000, jar(),
				MethodSignatures.ofNonVoid(C, "no2", StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
				c, StorageValues.intOf(13), StorageValues.intOf(17)));
	}

	@Test @DisplayName("install jar then call to View.yes() succeeds")
	void callYes() throws Exception {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), ConstructorSignatures.of(C));

		runInstanceNonVoidMethodCallTransaction(account(0), _100_000, jar(),
			MethodSignatures.ofNonVoid(C, "yes", StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
			c, StorageValues.intOf(13), StorageValues.intOf(17));
	}
}