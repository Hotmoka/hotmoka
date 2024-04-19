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

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.tests.HotmokaTest;

class Exceptions extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, IOException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("exceptions.jar"), takamakaCode());
	}

	@Test @DisplayName("install jar then calls foo1() and fails without program line")
	void callFoo1() throws TransactionException, IOException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		TransactionReference exceptions = addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("exceptions.jar"), takamakaCode());

		try {
			addStaticMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, exceptions, MethodSignatures.ofVoid("io.hotmoka.examples.errors.exceptions.C", "foo1"));
		}
		catch (Exception e) {
			assertTrue(e instanceof TransactionException);
			assertTrue(e.getMessage().startsWith(NullPointerException.class.getName()));
			assertTrue(e.getMessage().endsWith("@C.java:25"));
		}
	}

	@Test @DisplayName("install jar then calls foo2() and fails without program line")
	void callFoo2() throws TransactionException, IOException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		TransactionReference exceptions = addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("exceptions.jar"), takamakaCode());

		try {
			addStaticMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, exceptions, MethodSignatures.ofVoid("io.hotmoka.examples.errors.exceptions.C", "foo2", StorageTypes.OBJECT), StorageValues.NULL);
		}
		catch (Exception e) {
			assertTrue(e instanceof TransactionException);
			assertTrue(e.getMessage().startsWith(NullPointerException.class.getName()));
			assertTrue(e.getMessage().endsWith("@C.java:30"));
		}
	}
}