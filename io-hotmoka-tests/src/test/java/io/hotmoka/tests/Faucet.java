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
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.constants.Constants;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;

public class Faucet extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
	}

	@Test
	void fundNewAccount() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		if (consensus == null || !consensus.allowsUnsignedFaucet())
			return;

		StorageReference manifest = node.getManifest();
		StorageReference gamete = (StorageReference) runInstanceMethodCallTransaction(manifest, _50_000, takamakaCode(), MethodSignature.GET_GAMETE, manifest);

		// we generate the key pair of the new account created by the faucet
		SignatureAlgorithm<SignedTransactionRequest> signature = signature();
		KeyPair keys = signature.getKeyPair();
		String publicKey = Base64.getEncoder().encodeToString(signature().encodingOf(keys.getPublic()));

		// we use an arbitrary signature for calling the faucet, since it won't be checked
		Signer<SignedTransactionRequest> signer = signature.getSigner(signature.getKeyPair().getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

		var account = (StorageReference) node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(signer, gamete, getNonceOf(gamete), chainId, _100_000, ONE, takamakaCode(),
			new NonVoidMethodSignature(ClassType.GAMETE, "faucet", ClassType.EOA, BasicTypes.INT, ClassType.STRING),
			gamete, new IntValue(100_000), new StringValue(publicKey)));

		assertNotNull(account);
	}

	@Test
	void callToFaucetFailsIfCallerIsNotTheGamete() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException {
		StorageReference manifest = node.getManifest();
		var gamete = (StorageReference) runInstanceMethodCallTransaction(manifest, _50_000, takamakaCode(), MethodSignature.GET_GAMETE, manifest);

		// we generate the key pair of the new account created by the faucet
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.getEncoder().encodeToString(signature().encodingOf(keys.getPublic()));

		// we use an arbitrary signature for calling the faucet, since it won't be checked
		Signer<SignedTransactionRequest> signer = signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature);
		StorageReference caller = account(0);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(signer, caller, getNonceOf(caller), chainId, _50_000, ONE, takamakaCode(),
				new NonVoidMethodSignature(ClassType.GAMETE, "faucet", ClassType.EOA, BasicTypes.INT, ClassType.STRING),
				gamete, new IntValue(100_000), new StringValue(publicKey))));
	}
}