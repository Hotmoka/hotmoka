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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.KeyPair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.helpers.UnexpectedVoidMethodException;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.values.StorageReference;
import io.takamaka.code.constants.Constants;

public class Faucet extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
	}

	@Test
	void fundNewAccount() throws Exception {
		if (consensus == null || !consensus.allowsUnsignedFaucet())
			return;

		var gamete = runInstanceNonVoidMethodCallTransaction(manifest(), _500_000, takamakaCode(), MethodSignatures.GET_GAMETE, manifest()).asReturnedReference(MethodSignatures.GET_GAMETE, UnexpectedValueException::new);

		// we generate the key pair of the new account created by the faucet
		var signature = signature();
		KeyPair keys = signature.getKeyPair();
		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));

		// we use an arbitrary signature for calling the faucet, since it won't be checked
		Signer<SignedTransactionRequest<?>> signer = signature.getSigner(signature.getKeyPair().getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

		var method = MethodSignatures.ofNonVoid(StorageTypes.GAMETE, "faucet", StorageTypes.EOA, StorageTypes.INT, StorageTypes.STRING);
		var account = (StorageReference) node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
			(signer, gamete, getNonceOf(gamete), chainId(), _100_000, ONE, takamakaCode(),
			method, gamete, StorageValues.intOf(100_000), StorageValues.stringOf(publicKey)))
			.orElseThrow(() -> new UnexpectedVoidMethodException(method));

		assertNotNull(account);
	}

	@Test
	void callToFaucetFailsIfCallerIsNotTheGamete() throws Exception {
		var gamete = runInstanceNonVoidMethodCallTransaction(manifest(), _500_000, takamakaCode(), MethodSignatures.GET_GAMETE, manifest()).asReturnedReference(MethodSignatures.GET_GAMETE, UnexpectedValueException::new);

		// we generate the key pair of the new account created by the faucet
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));

		// we use an arbitrary signature for calling the faucet, since it won't be checked
		Signer<SignedTransactionRequest<?>> signer = signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature);
		StorageReference caller = account(0);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
				(signer, caller, getNonceOf(caller), chainId(), _500_000, ONE, takamakaCode(),
				MethodSignatures.ofNonVoid(StorageTypes.GAMETE, "faucet", StorageTypes.EOA, StorageTypes.INT, StorageTypes.STRING),
				gamete, StorageValues.intOf(100_000), StorageValues.stringOf(publicKey))));
	}
}