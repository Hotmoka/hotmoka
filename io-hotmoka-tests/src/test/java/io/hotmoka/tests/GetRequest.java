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

import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for {@link io.hotmoka.node.Node#getRequest(io.hotmoka.beans.references.TransactionReference)}.
 */
class GetRequest extends HotmokaTest {
	private static final ConstructorSignature ABSTRACT_FAIL_IMPL_CONSTRUCTOR = new ConstructorSignature(StorageTypes.classNamed("io.hotmoka.examples.abstractfail.AbstractFailImpl"), StorageTypes.INT);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("abstractfail.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("getRequest works for an existing transaction")
	void getRequest() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, new IntValue(42));
		TransactionRequest<?> request = getRequest(abstractfail.transaction);
		Assertions.assertTrue(request instanceof ConstructorCallTransactionRequest);
		Assertions.assertEquals(account(0), ((ConstructorCallTransactionRequest) request).caller);
	}

	@Test @DisplayName("getRequest works for a non-existing transaction")
	void getRequestNonExisting() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		try {
			StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, new IntValue(42));
			String hash = abstractfail.transaction.getHash();
			// re replace the first digit: the resulting transaction reference does not exist
			char digit = (hash.charAt(0) == '0') ? '1' : '0';
			hash = digit + hash.substring(1);
			getRequest(new LocalTransactionReference(hash));
			fail("missing exception");
		}
		catch (NoSuchElementException e) {
		}
	}
}