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
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithms;

/**
 * A test for signing transactions with distinct signatures.
 */
class Signatures extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("create accounts with distinct signing algorithms")
	void createAccountsWithDistinctSigningAlgorithms() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		var amount = new IntValue(_10_000_000.intValue());

		var sha256dsa = SignatureAlgorithms.sha256dsa();
		KeyPair sha256dsaKeyPair = sha256dsa.getKeyPair();
		var sha256dsaPublicKey = new StringValue(Base64.getEncoder().encodeToString(sha256dsa.encodingOf(sha256dsaKeyPair.getPublic())));
		addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountSHA256DSA", StorageTypes.INT, StorageTypes.STRING), amount, sha256dsaPublicKey);

		var qtesla1 = SignatureAlgorithms.qtesla1();
		KeyPair qteslaKeyPair = qtesla1.getKeyPair();
		var qteslaPublicKey = new StringValue(Base64.getEncoder().encodeToString(qtesla1.encodingOf(qteslaKeyPair.getPublic())));
		addConstructorCallTransaction(privateKey(0), account(0), _10_000_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1", StorageTypes.INT, StorageTypes.STRING), amount, qteslaPublicKey);

		var ed25519 = SignatureAlgorithms.ed25519();
		KeyPair ed25519KeyPair = ed25519.getKeyPair();
		var ed25519PublicKey = new StringValue(Base64.getEncoder().encodeToString(ed25519.encodingOf(ed25519KeyPair.getPublic())));
		addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountED25519", StorageTypes.INT, StorageTypes.STRING), amount, ed25519PublicKey);
	}

	@Test @DisplayName("create accounts with distinct signing algorithms and use them for signing transactions")
	void createAccountsWithDistinctSigningAlgorithmsAndUseThem() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		var amount = new IntValue(_10_000_000.intValue());
		var callee = new NonVoidMethodSignature("io.takamaka.code.lang.Coin", "panarea", StorageTypes.BIG_INTEGER, StorageTypes.LONG);

		var sha256dsa = SignatureAlgorithms.sha256dsa();
		KeyPair sha256dsaKeyPair = sha256dsa.getKeyPair();
		var sha256dsaPublicKey = new StringValue(Base64.getEncoder().encodeToString(sha256dsa.encodingOf(sha256dsaKeyPair.getPublic())));
		StorageReference sha256dsaAccount = addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountSHA256DSA", StorageTypes.INT, StorageTypes.STRING), amount, sha256dsaPublicKey);
		var sha256dsaResult = (BigIntegerValue) node.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(sha256dsa.getSigner(sha256dsaKeyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature), sha256dsaAccount, ZERO, chainId, _100_000, ONE, takamakaCode(), callee, new LongValue(1973)));
		assertEquals(BigInteger.valueOf(1973), sha256dsaResult.value);

		var qtesla1 = SignatureAlgorithms.qtesla1();
		KeyPair qteslaKeyPair = qtesla1.getKeyPair();
		var qteslaPublicKey = new StringValue(Base64.getEncoder().encodeToString(qtesla1.encodingOf(qteslaKeyPair.getPublic())));
		StorageReference qteslaAccount = addConstructorCallTransaction(privateKey(0), account(0), _10_000_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1", StorageTypes.INT, StorageTypes.STRING), amount, qteslaPublicKey);
		var qteslaResult = (BigIntegerValue) node.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(qtesla1.getSigner(qteslaKeyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature), qteslaAccount, ZERO, chainId, _500_000, ONE, takamakaCode(), callee, new LongValue(1973)));
		assertEquals(BigInteger.valueOf(1973), qteslaResult.value);

		var ed25519 = SignatureAlgorithms.ed25519();
		KeyPair ed25519KeyPair = ed25519.getKeyPair();
		var ed25519PublicKey = new StringValue(Base64.getEncoder().encodeToString(ed25519.encodingOf(ed25519KeyPair.getPublic())));
		StorageReference ed25519Account = addConstructorCallTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountED25519", StorageTypes.INT, StorageTypes.STRING), amount, ed25519PublicKey);
		var ed25519Result = (BigIntegerValue) node.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(ed25519.getSigner(ed25519KeyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature), ed25519Account, ZERO, chainId, _500_000, ONE, takamakaCode(), callee, new LongValue(1973)));
		assertEquals(BigInteger.valueOf(1973), ed25519Result.value);
	}
}