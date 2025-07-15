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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.OutOfGasException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.tests.HotmokaTest;

class Exceptions extends HotmokaTest {

	private final static ClassType C = StorageTypes.classNamed("io.hotmoka.examples.errors.exceptions.C");
	private final static ClassType BIG_ARRAY = StorageTypes.classNamed("io.hotmoka.examples.errors.exceptions.BigArray");

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws Exception {
		addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("exceptions.jar"), takamakaCode());
	}

	@Test @DisplayName("install jar then calls foo1() and fails without program line")
	void callFoo1() throws Exception {
		TransactionReference exceptions = addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("exceptions.jar"), takamakaCode());

		try {
			addStaticVoidMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, exceptions, MethodSignatures.ofVoid(C, "foo1"));
		}
		catch (Exception e) {
			assertTrue(e instanceof TransactionException);
			assertTrue(e.getMessage().startsWith(NullPointerException.class.getName()));
			assertTrue(e.getMessage().endsWith("@C.java:27"));
		}
	}

	@Test @DisplayName("install jar then calls foo2() and fails with program line")
	void callFoo2() throws Exception {
		TransactionReference exceptions = addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("exceptions.jar"), takamakaCode());

		try {
			addStaticVoidMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, exceptions, MethodSignatures.ofVoid(C, "foo2", StorageTypes.EOA), StorageValues.NULL);
		}
		catch (Exception e) {
			assertTrue(e instanceof TransactionException);
			assertTrue(e.getMessage().startsWith(NullPointerException.class.getName()));
			assertTrue(e.getMessage().endsWith("@C.java:32"));
		}
	}

	@Test @DisplayName("install jar then calls new BigArray() and fails for lack of gas")
	void createBigArrayFailsForLackOfGas() throws Exception {
		TransactionReference exceptions = addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("exceptions.jar"), takamakaCode());

		try {
			addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, exceptions, ConstructorSignatures.of(BIG_ARRAY));
		}
		catch (Exception e) {
			assertTrue(e instanceof TransactionException);
			assertTrue(e.getMessage().startsWith(OutOfGasException.class.getName()));
			assertTrue(e.getMessage().endsWith("@BigArray.java:23"));
		}
	}
}