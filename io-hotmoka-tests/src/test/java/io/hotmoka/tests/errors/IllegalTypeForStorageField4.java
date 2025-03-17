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
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.local.internal.builders.UpdatesExtractionException;
import io.hotmoka.tests.HotmokaTest;

class IllegalTypeForStorageField4 extends HotmokaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("illegaltypeforstoragefield4.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("storing non-storage into interface field fails")
	void triesToStoreNonStorageIntoInterfaceField() {
		throwsTransactionExceptionWithCause(UpdatesExtractionException.class, () ->
			addConstructorCallTransaction
				(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(),
				ConstructorSignatures.of(StorageTypes.classNamed("io.hotmoka.examples.errors.illegaltypeforstoragefield4.C")))
		);
	}
}