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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.NonWhiteListedCallException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * A test for the Java HashMap class.
 */
class JavaCollections extends HotmokaTest {
	private static final ClassType HASH_MAP_TESTS = StorageTypes.classNamed("io.hotmoka.examples.javacollections.HashMapTests");
	private static final ClassType HASH_SET_TESTS = StorageTypes.classNamed("io.hotmoka.examples.javacollections.HashSetTests");

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("javacollections.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("HashMapTests.testToString1() == [how, are, hello, you, ?]")
	void toString1OnHashMap() throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		StringValue toString = (StringValue) runStaticMethodCallTransaction
			(account(0), _1_000_000, jar(), MethodSignatures.of(HASH_MAP_TESTS, "testToString1", StorageTypes.STRING));
		assertEquals("[how, are, hello, you, ?]", toString.getValue());
	}

	@Test @DisplayName("HashMapTests.testToString2() == [how, are, hello, you, ?]")
	void toString2OnHashMap() throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		StringValue toString = (StringValue) runStaticMethodCallTransaction
			(account(0), _1_000_000, jar(), MethodSignatures.of(HASH_MAP_TESTS, "testToString2", StorageTypes.STRING));
		assertEquals("[how, are, hello, you, ?]", toString.getValue());
	}

	@Test @DisplayName("HashMapTests.testToString3() fails with a run-time white-listing violation")
	void toString3OnHashMap() {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			runStaticMethodCallTransaction(account(0), _1_000_000, jar(), MethodSignatures.of(HASH_MAP_TESTS, "testToString3", StorageTypes.STRING))
		);
	}

	@Test @DisplayName("HashMapTests.testToString4() == [how, are, hello, you, ?]")
	void toString4OnHashMap() throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		StringValue toString = (StringValue) runStaticMethodCallTransaction
			(account(0), _1_000_000, jar(), MethodSignatures.of(HASH_MAP_TESTS, "testToString4", StorageTypes.STRING));
		assertEquals("[are, io.hotmoka.examples.javacollections.C@2a, hello, you, ?]", toString.getValue());
	}

	@Test @DisplayName("HashSetTests.testToString1() == [how, are, hello, you, ?]")
	void toString1OnHashSet() throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		StringValue toString = (StringValue) runStaticMethodCallTransaction
			(account(0), _1_000_000, jar(), MethodSignatures.of(HASH_SET_TESTS, "testToString1", StorageTypes.STRING));
		assertEquals("[how, are, hello, you, ?]", toString.getValue());
	}

	@Test @DisplayName("HashSetTests.testToString2() == [how, are, hello, you, ?]")
	void toString2OnHashSet() {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			runStaticMethodCallTransaction(account(0), _1_000_000, jar(), MethodSignatures.of(HASH_SET_TESTS, "testToString2", StorageTypes.STRING))
		);
	}

	@Test @DisplayName("HashSetTests.testToString3() == [how, are, hello, you, ?]")
	void toString3OnHashSet() throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		StringValue toString = (StringValue) runStaticMethodCallTransaction
			(account(0), _1_000_000, jar(), MethodSignatures.of(HASH_SET_TESTS, "testToString3", StorageTypes.STRING));
		assertEquals("[how, are, io.hotmoka.examples.javacollections.C@2a, hello, you, ?]", toString.getValue());
	}
}