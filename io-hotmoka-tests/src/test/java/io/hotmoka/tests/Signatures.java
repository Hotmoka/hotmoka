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
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.KeyPair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * A test for signing transactions with distinct signatures.
 */
class Signatures extends HotmokaTest {

	private final static ClassType SHA256DSA = StorageTypes.classNamed("io.takamaka.code.lang.ExternallyOwnedAccountSHA256DSA");
	private final static ClassType QTESLA1 = StorageTypes.classNamed("io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1");
	private final static ClassType ED25519 = StorageTypes.classNamed("io.takamaka.code.lang.ExternallyOwnedAccountED25519");

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("create accounts with distinct signing algorithms")
	void createAccountsWithDistinctSigningAlgorithms() throws Exception {
		var amount = StorageValues.intOf(_10_000_000.intValue());

		var sha256dsa = SignatureAlgorithms.sha256dsa();
		KeyPair sha256dsaKeyPair = sha256dsa.getKeyPair();
		var sha256dsaPublicKey = StorageValues.stringOf(Base64.toBase64String(sha256dsa.encodingOf(sha256dsaKeyPair.getPublic())));
		addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), ConstructorSignatures.of(SHA256DSA, StorageTypes.INT, StorageTypes.STRING), amount, sha256dsaPublicKey);

		var qtesla1 = SignatureAlgorithms.qtesla1();
		KeyPair qteslaKeyPair = qtesla1.getKeyPair();
		var qteslaPublicKey = StorageValues.stringOf(Base64.toBase64String(qtesla1.encodingOf(qteslaKeyPair.getPublic())));
		addConstructorCallTransaction(privateKey(0), account(0), _10_000_000, ONE, takamakaCode(), ConstructorSignatures.of(QTESLA1, StorageTypes.INT, StorageTypes.STRING), amount, qteslaPublicKey);

		var ed25519 = SignatureAlgorithms.ed25519();
		KeyPair ed25519KeyPair = ed25519.getKeyPair();
		var ed25519PublicKey = StorageValues.stringOf(Base64.toBase64String(ed25519.encodingOf(ed25519KeyPair.getPublic())));
		addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), ConstructorSignatures.of(ED25519, StorageTypes.INT, StorageTypes.STRING), amount, ed25519PublicKey);
	}

	@Test @DisplayName("create accounts with distinct signing algorithms and use them for signing transactions")
	void createAccountsWithDistinctSigningAlgorithmsAndUseThem() throws Exception {
		var amount = StorageValues.intOf(_10_000_000.intValue());
		var callee = MethodSignatures.ofNonVoid(StorageTypes.classNamed("io.takamaka.code.lang.Coin"), "panarea", StorageTypes.BIG_INTEGER, StorageTypes.LONG);

		var sha256dsa = SignatureAlgorithms.sha256dsa();
		KeyPair sha256dsaKeyPair = sha256dsa.getKeyPair();
		var sha256dsaPublicKey = StorageValues.stringOf(Base64.toBase64String(sha256dsa.encodingOf(sha256dsaKeyPair.getPublic())));
		StorageReference sha256dsaAccount = addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), ConstructorSignatures.of(SHA256DSA, StorageTypes.INT, StorageTypes.STRING), amount, sha256dsaPublicKey);
		var sha256dsaResult = node.addStaticMethodCallTransaction(TransactionRequests.staticMethodCall
			(sha256dsa.getSigner(sha256dsaKeyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature), sha256dsaAccount, ZERO, chainId(),
			_100_000, ONE, takamakaCode(), callee, new StorageValue[] { StorageValues.longOf(1973) }, IllegalArgumentException::new)).get().asReturnedBigInteger(callee, NodeException::new);
		assertEquals(BigInteger.valueOf(1973), sha256dsaResult);

		var qtesla1 = SignatureAlgorithms.qtesla1();
		KeyPair qteslaKeyPair = qtesla1.getKeyPair();
		var qteslaPublicKey = StorageValues.stringOf(Base64.toBase64String(qtesla1.encodingOf(qteslaKeyPair.getPublic())));
		StorageReference qteslaAccount = addConstructorCallTransaction(privateKey(0), account(0), _10_000_000, ONE, takamakaCode(), ConstructorSignatures.of(QTESLA1, StorageTypes.INT, StorageTypes.STRING), amount, qteslaPublicKey);
		var qteslaResult = node.addStaticMethodCallTransaction(TransactionRequests.staticMethodCall
			(qtesla1.getSigner(qteslaKeyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature), qteslaAccount, ZERO, chainId(),
			_500_000, ONE, takamakaCode(), callee, new StorageValue[] {  StorageValues.longOf(1973) }, IllegalArgumentException::new)).get().asReturnedBigInteger(callee, NodeException::new);
		assertEquals(BigInteger.valueOf(1973), qteslaResult);

		var ed25519 = SignatureAlgorithms.ed25519();
		KeyPair ed25519KeyPair = ed25519.getKeyPair();
		var ed25519PublicKey = StorageValues.stringOf(Base64.toBase64String(ed25519.encodingOf(ed25519KeyPair.getPublic())));
		StorageReference ed25519Account = addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), ConstructorSignatures.of(ED25519, StorageTypes.INT, StorageTypes.STRING), amount, ed25519PublicKey);
		var ed25519Result = node.addStaticMethodCallTransaction(TransactionRequests.staticMethodCall
			(ed25519.getSigner(ed25519KeyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature), ed25519Account, ZERO, chainId(),
			_500_000, ONE, takamakaCode(), callee, new StorageValue[] {  StorageValues.longOf(1973) }, IllegalArgumentException::new)).get().asReturnedBigInteger(callee, NodeException::new);
		assertEquals(BigInteger.valueOf(1973), ed25519Result);
	}
}