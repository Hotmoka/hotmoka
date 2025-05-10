/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.moka.KeysCreateOutputs;
import io.hotmoka.moka.Moka;
import io.hotmoka.moka.NodesTendermintValidatorsCreateOutputs;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.NonVoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.values.StorageReference;

/**
 * Tests for the moka nodes tendermint validators command.
 */
public class NodesTendermintValidatorsTests extends AbstractMokaTestWithNode {
	
	@Test
	@DisplayName("[moka nodes tendermint validators create] the creation of new validator account from the faucet works")
	public void validatorAccountCreationFromFaucetWorks() throws Exception {
		var signature = SignatureAlgorithms.ed25519();
		String passwordOfNewAccount = "abcde";

		// first we create a key pair
		var keyCreateOutputs = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir));
		// then we create a new validator account with that key pair, and let the faucet pay for it
		var accountsCreateOutput = NodesTendermintValidatorsCreateOutputs.from(Moka.nodesTendermintValidatorsCreate("faucet 12345 --keys=" + keyCreateOutputs.getFile() + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		TransactionResponse response = node.getResponse(accountsCreateOutput.getTransaction());

		// the response in the output of the command should be successful
		assertTrue(response instanceof NonVoidMethodCallTransactionSuccessfulResponse);
		NonVoidMethodCallTransactionSuccessfulResponse successfulResponse = (NonVoidMethodCallTransactionSuccessfulResponse) response;

		// the created account is reported in the output
		assertTrue(accountsCreateOutput.getAccount().isPresent());
		StorageReference account = accountsCreateOutput.getAccount().get();

		// that response must have, as result, the created account
		assertEquals(accountsCreateOutput.getAccount().get(), successfulResponse.getResult());

		// the created account is a Tendermint validator account
		assertEquals(StorageTypes.TENDERMINT_ED25519_VALIDATOR, node.getClassTag(account).getClazz());

		// the new account must have been created in the reported transaction
		assertEquals(accountsCreateOutput.getTransaction(), account.getTransaction());

		BigInteger balance = node.runInstanceMethodCallTransaction(
				TransactionRequests.instanceViewMethodCall(gamete, _100_000, takamakaCode, MethodSignatures.BALANCE, account))
				.orElseThrow(() -> new IllegalStateException(MethodSignatures.BALANCE + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.BALANCE, IllegalStateException::new);

		// the balance in the account must be as required
		assertEquals(BigInteger.valueOf(12345), balance);

		String publicKeyBase64 = node.runInstanceMethodCallTransaction(
				TransactionRequests.instanceViewMethodCall(gamete, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, account))
				.orElseThrow(() -> new IllegalStateException(MethodSignatures.PUBLIC_KEY + " should not return void"))
				.asReturnedString(MethodSignatures.PUBLIC_KEY, IllegalStateException::new);

		var keyPairOfNewAccount = Entropies.load(accountsCreateOutput.getFile().get()).keys(passwordOfNewAccount, signature);
		byte[] encodingOfPublicKeyOfNewAccount = signature.encodingOf(keyPairOfNewAccount.getPublic());
		// the public key in the account must be as reported in the created key pair file
		assertEquals(Base64.toBase64String(encodingOfPublicKeyOfNewAccount), publicKeyBase64);
	}

	@Test
	@DisplayName("[moka nodes tendermint validators create] the creation of new validator account from another account works")
	public void validatorAccountCreationFromOtherAccountWorks() throws Exception {
		var signature = SignatureAlgorithms.ed25519();
		String passwordOfNewAccount = "abcde";

		// first we create a key pair
		var keyCreateOutputs = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir));
		// then we create a new validator account with that key pair, and let the gamete for it
		var accountsCreateOutput = NodesTendermintValidatorsCreateOutputs.from(Moka.nodesTendermintValidatorsCreate(gamete + " 12345 --password-of-payer=" + passwordOfGamete + " --dir=" + dir + " --keys=" + keyCreateOutputs.getFile() + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		TransactionResponse response = node.getResponse(accountsCreateOutput.getTransaction());

		// the response in the output of the command should be successful
		assertTrue(response instanceof ConstructorCallTransactionSuccessfulResponse);
		ConstructorCallTransactionSuccessfulResponse successfulResponse = (ConstructorCallTransactionSuccessfulResponse) response;

		// the created account is reported in the output
		assertTrue(accountsCreateOutput.getAccount().isPresent());
		StorageReference account = accountsCreateOutput.getAccount().get();

		// that response must have, as result, the created account
		assertEquals(account, successfulResponse.getNewObject());

		// the created account is a Tendermint validator account
		assertEquals(StorageTypes.TENDERMINT_ED25519_VALIDATOR, node.getClassTag(account).getClazz());

		// the new account must have been created in the reported transaction
		assertEquals(accountsCreateOutput.getTransaction(), account.getTransaction());

		BigInteger balance = node.runInstanceMethodCallTransaction(
				TransactionRequests.instanceViewMethodCall(gamete, _100_000, takamakaCode, MethodSignatures.BALANCE, account))
				.orElseThrow(() -> new IllegalStateException(MethodSignatures.BALANCE + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.BALANCE, IllegalStateException::new);

		// the balance in the account must be as required
		assertEquals(BigInteger.valueOf(12345), balance);

		String publicKeyBase64 = node.runInstanceMethodCallTransaction(
				TransactionRequests.instanceViewMethodCall(gamete, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, account))
				.orElseThrow(() -> new IllegalStateException(MethodSignatures.PUBLIC_KEY + " should not return void"))
				.asReturnedString(MethodSignatures.PUBLIC_KEY, IllegalStateException::new);

		var keyPairOfNewAccount = Entropies.load(accountsCreateOutput.getFile().get()).keys(passwordOfNewAccount, signature);
		byte[] encodingOfPublicKeyOfNewAccount = signature.encodingOf(keyPairOfNewAccount.getPublic());
		// the public key in the account must be as reported in the created key pair file
		assertEquals(Base64.toBase64String(encodingOfPublicKeyOfNewAccount), publicKeyBase64);
	}
}