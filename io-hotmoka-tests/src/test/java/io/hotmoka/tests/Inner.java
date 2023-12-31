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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.internal.BasicTypes;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for inner classes.
 */
class Inner extends HotmokaTest {
	private static final ConstructorSignature TEST_INNER_CONSTRUCTOR = new ConstructorSignature("io.hotmoka.examples.inner.TestInner");

	// do not forget the implicit parameter holding the parent of the inner object
	private static final ConstructorSignature TEST_INNER_INSIDE_CONSTRUCTOR = new ConstructorSignature("io.hotmoka.examples.inner.TestInner$Inside",
			StorageTypes.classNamed("io.hotmoka.examples.inner.TestInner"), BasicTypes.LONG);

	private static final NonVoidMethodSignature TEST_INNER_INSIDE_GETBALANCE = new NonVoidMethodSignature("io.hotmoka.examples.inner.TestInner$Inside", "getBalance", StorageTypes.BIG_INTEGER);

	private static final NonVoidMethodSignature TEST_INNER_INSIDE_GETPARENT = new NonVoidMethodSignature("io.hotmoka.examples.inner.TestInner$Inside", "getParent",
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
	void newTestInner() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), TEST_INNER_CONSTRUCTOR);
	}

	@Test @DisplayName("(new TestInner().new Inside(1000)).getBalance() == 1000")
	void newTestInnerInsideGetBalance() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference testInner = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), TEST_INNER_CONSTRUCTOR);
		StorageReference inside = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), TEST_INNER_INSIDE_CONSTRUCTOR, testInner, new LongValue(1000L));
		BigIntegerValue balance = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), TEST_INNER_INSIDE_GETBALANCE, inside);
		
		assertEquals(balance.value, BigInteger.valueOf(1000L));
	}

	@Test @DisplayName("ti = new TestInner(); (ti.new Inside(1000)).getParent() == ti")
	void newTestInnerInsideGetParent() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference testInner = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), TEST_INNER_CONSTRUCTOR);
		StorageReference inside = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), TEST_INNER_INSIDE_CONSTRUCTOR, testInner, new LongValue(1000L));
		StorageReference parent = (StorageReference) runInstanceMethodCallTransaction(account(0), _50_000, jar(), TEST_INNER_INSIDE_GETPARENT, inside);
		
		assertEquals(testInner, parent);
	}
}