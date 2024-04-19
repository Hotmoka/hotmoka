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
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for inner classes.
 */
class Inner extends HotmokaTest {
	private static final ConstructorSignature TEST_INNER_CONSTRUCTOR = ConstructorSignatures.of("io.hotmoka.examples.inner.TestInner");

	// do not forget the implicit parameter holding the parent of the inner object
	private static final ConstructorSignature TEST_INNER_INSIDE_CONSTRUCTOR = ConstructorSignatures.of("io.hotmoka.examples.inner.TestInner$Inside",
			StorageTypes.classNamed("io.hotmoka.examples.inner.TestInner"), StorageTypes.LONG);

	private static final NonVoidMethodSignature TEST_INNER_INSIDE_GETBALANCE = MethodSignatures.ofNonVoid("io.hotmoka.examples.inner.TestInner$Inside", "getBalance", StorageTypes.BIG_INTEGER);

	private static final NonVoidMethodSignature TEST_INNER_INSIDE_GETPARENT = MethodSignatures.ofNonVoid("io.hotmoka.examples.inner.TestInner$Inside", "getParent",
			StorageTypes.classNamed("io.hotmoka.examples.inner.TestInner"));

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("inner.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_100_000);
	}

	@Test @DisplayName("new TestInner()")
	void newTestInner() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), TEST_INNER_CONSTRUCTOR);
	}

	@Test @DisplayName("(new TestInner().new Inside(1000)).getBalance() == 1000")
	void newTestInnerInsideGetBalance() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference testInner = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), TEST_INNER_CONSTRUCTOR);
		StorageReference inside = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), TEST_INNER_INSIDE_CONSTRUCTOR, testInner, StorageValues.longOf(1000L));
		BigIntegerValue balance = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), TEST_INNER_INSIDE_GETBALANCE, inside);
		
		assertEquals(balance.getValue(), BigInteger.valueOf(1000L));
	}

	@Test @DisplayName("ti = new TestInner(); (ti.new Inside(1000)).getParent() == ti")
	void newTestInnerInsideGetParent() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference testInner = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), TEST_INNER_CONSTRUCTOR);
		StorageReference inside = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), TEST_INNER_INSIDE_CONSTRUCTOR, testInner, StorageValues.longOf(1000L));
		StorageReference parent = (StorageReference) runInstanceMethodCallTransaction(account(0), _50_000, jar(), TEST_INNER_INSIDE_GETPARENT, inside);
		
		assertEquals(testInner, parent);
	}
}