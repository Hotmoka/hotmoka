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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * A test that split packages are not allowed.
 */
class SplitPackage extends HotmokaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("basicdependency.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, BigInteger.ZERO);
	}

	@Test @DisplayName("jars with distinct packages coexist")
	void testDisjointJars() throws TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException, IOException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		addJarStoreTransaction(privateKey(0), account(0), _1_000_000, ONE, takamakaCode(), bytesOf("basic.jar"), jar());
	}
	
	@Test @DisplayName("jars with packages split among them cannot be put together")
	void testSplitPackages() {
		assertThrows(TransactionRejectedException.class, () ->
			addJarStoreTransaction(privateKey(0), account(0), _1_000_000, ONE, takamakaCode(), bytesOf("basicdependency.jar"), jar()));
	}
}