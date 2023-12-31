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
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A test for from contract methods in a storage class.
 */
class FromContractsInStorage extends HotmokaTest {
	private final static ClassType FROM_CONTRACTS = StorageTypes.classNamed("io.hotmoka.examples.fromcontractsinstorage.FromContracts");

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("fromcontractsinstorage.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
	}

	@Test @DisplayName("new FromContracts().entry1() yields the eoa that calls the transaction")
	void callFromContract1() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference entries = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(FROM_CONTRACTS));
		StorageValue result = addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new NonVoidMethodSignature(FROM_CONTRACTS, "entry1", StorageTypes.CONTRACT), entries);
		assertEquals(account(0), result);
	}

	@Test @DisplayName("new FromContracts().entry5() yields the eoa that calls the transaction")
	void callFromContract5() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference entries = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(FROM_CONTRACTS));
		StorageValue result = addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new NonVoidMethodSignature(FROM_CONTRACTS, "entry5", StorageTypes.CONTRACT), entries);
		assertEquals(account(0), result);
	}
}