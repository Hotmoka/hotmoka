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

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * A test for {@link io.hotmoka.node.Node#getResponse(io.hotmoka.beans.references.TransactionReference)}.
 */
class GetResponse extends HotmokaTest {
	private static final ConstructorSignature ABSTRACT_FAIL_IMPL_CONSTRUCTOR = ConstructorSignatures.of(StorageTypes.classNamed("io.hotmoka.examples.abstractfail.AbstractFailImpl"), StorageTypes.INT);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("abstractfail.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("getResponse works for an existing transaction")
	void getResponse() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, StorageValues.intOf(42));
		TransactionResponse response = getResponse(abstractfail.getTransaction());
		Assertions.assertTrue(response instanceof ConstructorCallTransactionResponse);
	}

	@Test @DisplayName("getResponse works for a non-existing transaction")
	void getResponseNonExisting() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		try {
			StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, StorageValues.intOf(42));
			byte[] hash = abstractfail.getTransaction().getHash();
			// we modify the first byte: the resulting transaction reference does not exist
			hash[0]++;
			getResponse(TransactionReferences.of(hash));
			fail("missing exception");
		}
		catch (NoSuchElementException e) {
		}
	}
}