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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for {@link io.hotmoka.node.api.Node#getRequest(io.hotmoka.beans.api.transactions.TransactionReference)}.
 */
public class GetRequest extends HotmokaTest {
	private static final ConstructorSignature ABSTRACT_FAIL_IMPL_CONSTRUCTOR = ConstructorSignatures.of
		(StorageTypes.classNamed("io.hotmoka.examples.abstractfail.AbstractFailImpl", IllegalArgumentException::new), StorageTypes.INT);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("abstractfail.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("getRequest works for an existing transaction")
	public void getRequest() throws Exception {
		StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, StorageValues.intOf(42));
		TransactionRequest<?> request = getRequest(abstractfail.getTransaction());
		assertTrue(request instanceof ConstructorCallTransactionRequest);
		assertEquals(account(0), ((ConstructorCallTransactionRequest) request).getCaller());
	}

	@Test @DisplayName("getRequest works for a non-existing transaction")
	public void getRequestNonExisting() throws Exception {
		StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, StorageValues.intOf(42));
		byte[] hash = abstractfail.getTransaction().getHash();
		// we modify the first byte: the resulting transaction reference does not exist
		hash[0]++;
		assertThrows(UnknownReferenceException.class, () -> getRequest(TransactionReferences.of(hash, IllegalArgumentException::new)));
	}
}