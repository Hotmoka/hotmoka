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

import static io.hotmoka.helpers.Coin.panarea;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageValue;

/**
 * A test for the installation in the node of a class with the same name
 * as a class of the same node.
 */
class SynonymClass extends HotmokaTest {
	private final static ClassType SA = StorageTypes.classNamed("io.hotmoka.crypto.SignatureAlgorithm", IllegalArgumentException::new);
	private final static NonVoidMethodSignature EMPTY = MethodSignatures.ofNonVoid(SA, "empty", SA);
	private final static BigInteger _20_000 = BigInteger.valueOf(20_000);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("crypto.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, BigInteger.valueOf(100_000L), BigInteger.valueOf(1_000_000L));
	}

	@Test @DisplayName("SignatureAlgorithm.empty() yields null")
	void createAbstractFail() throws Exception {
		StorageValue result = addStaticNonVoidMethodCallTransaction(privateKey(0), account(0), _20_000, panarea(1), jar(), EMPTY);
		// we verify that the result is null; this means that the implementation of SignatureAlgorithm
		// from the store of the node has been run, not that used by the node
		assertSame(StorageValues.NULL, result);
	}
}