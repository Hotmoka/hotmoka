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

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.requests.SignedTransactionRequest;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.takamaka.code.constants.Constants;

public class Faucet extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
	}

	@Test
	void fundNewAccount() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		if (consensus == null || !consensus.allowsUnsignedFaucet())
			return;

		StorageReference gamete = (StorageReference) runInstanceMethodCallTransaction(manifest(), _50_000, takamakaCode(), MethodSignatures.GET_GAMETE, manifest());

		// we generate the key pair of the new account created by the faucet
		var signature = signature();
		KeyPair keys = signature.getKeyPair();
		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));

		// we use an arbitrary signature for calling the faucet, since it won't be checked
		Signer<SignedTransactionRequest<?>> signer = signature.getSigner(signature.getKeyPair().getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

		var account = (StorageReference) node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
			(signer, gamete, getNonceOf(gamete), chainId, _100_000, ONE, takamakaCode(),
			MethodSignatures.of(StorageTypes.GAMETE, "faucet", StorageTypes.EOA, StorageTypes.INT, StorageTypes.STRING),
			gamete, StorageValues.intOf(100_000), StorageValues.stringOf(publicKey)));

		assertNotNull(account);
	}

	@Test
	void callToFaucetFailsIfCallerIsNotTheGamete() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		var gamete = (StorageReference) runInstanceMethodCallTransaction(manifest(), _50_000, takamakaCode(), MethodSignatures.GET_GAMETE, manifest());

		// we generate the key pair of the new account created by the faucet
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));

		// we use an arbitrary signature for calling the faucet, since it won't be checked
		Signer<SignedTransactionRequest<?>> signer = signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature);
		StorageReference caller = account(0);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
				(signer, caller, getNonceOf(caller), chainId, _50_000, ONE, takamakaCode(),
				MethodSignatures.of(StorageTypes.GAMETE, "faucet", StorageTypes.EOA, StorageTypes.INT, StorageTypes.STRING),
				gamete, StorageValues.intOf(100_000), StorageValues.stringOf(publicKey))));
	}
}