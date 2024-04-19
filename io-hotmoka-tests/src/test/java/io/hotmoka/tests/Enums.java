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

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.EnumValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the use of enumeration types.
 */
class Enums extends HotmokaTest {
	private static final ClassType MY_ENUM = StorageTypes.classNamed("io.hotmoka.examples.enums.MyEnum");

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("enums.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, _100_000);
	}

	@Test @DisplayName("new TestEnums(MyEnum.PRESENT)")
	void testEnumAsActual() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		addConstructorCallTransaction(privateKey(0), account(0), _10_000_000, BigInteger.ONE, jar(),
				ConstructorSignatures.of("io.hotmoka.examples.enums.TestEnums", MY_ENUM), StorageValues.enumElementOf("io.hotmoka.examples.enums.MyEnum", "PRESENT"));
	}

	@Test @DisplayName("new TestEnums(MyEnum.PRESENT).getOrdinal() == 1")
	void testGetOrdinal() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference testEnums = addConstructorCallTransaction(privateKey(0), account(0), _10_000_000, ONE, jar(),
			ConstructorSignatures.of("io.hotmoka.examples.enums.TestEnums", MY_ENUM), StorageValues.enumElementOf("io.hotmoka.examples.enums.MyEnum", "PRESENT"));

		IntValue ordinal = (IntValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(),
			MethodSignatures.ofNonVoid("io.hotmoka.examples.enums.TestEnums", "getOrdinal", StorageTypes.INT), testEnums);

		assertSame(1, ordinal.getValue());
	}

	@Test @DisplayName("TestEnums.getFor(2) == MyEnum.FUTURE")
	void testGetFor() throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		EnumValue element = (EnumValue) runStaticMethodCallTransaction(account(0), _50_000, jar(),
			MethodSignatures.ofNonVoid("io.hotmoka.examples.enums.TestEnums", "getFor", MY_ENUM, StorageTypes.INT), StorageValues.intOf(2));

		assertEquals(StorageValues.enumElementOf(MY_ENUM.getName(), "FUTURE"), element);
	}
}