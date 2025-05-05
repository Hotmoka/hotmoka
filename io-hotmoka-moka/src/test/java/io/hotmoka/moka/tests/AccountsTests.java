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

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.moka.AccountsCreateOutputs;
import io.hotmoka.moka.AccountsShowOutputs;
import io.hotmoka.moka.KeysCreateOutputs;
import io.hotmoka.moka.MokaNew;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.NonVoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;

/**
 * Tests for the moka nodes command.
 */
public class AccountsTests extends AbstractMokaTestWithNode {
	
	@Test
	@DisplayName("[moka accounts create] the creation of new account from the faucet works")
	public void accountCreationFromFaucetWorks() throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		String passwordOfNewAccount = "abcde";

		// first we create a key pair
		var keyCreateOutputs = KeysCreateOutputs.from(MokaNew.keysCreate("--signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir));
		// then we create a new account with that key pair, and let the faucet pay for it
		var accountsCreateOutput = AccountsCreateOutputs.from(MokaNew.accountsCreate("12345 --keys=" + keyCreateOutputs.getFile() + " --signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		TransactionResponse response = node.getResponse(accountsCreateOutput.getTransaction());

		// the response in the output of the command should be successful
		assertTrue(response instanceof NonVoidMethodCallTransactionSuccessfulResponse);
		NonVoidMethodCallTransactionSuccessfulResponse successfulResponse = (NonVoidMethodCallTransactionSuccessfulResponse) response;

		// that response must have, as result, the created account
		assertEquals(accountsCreateOutput.getAccount(), successfulResponse.getResult());

		BigInteger balance = node.runInstanceMethodCallTransaction(
				TransactionRequests.instanceViewMethodCall(gamete, _100_000, takamakaCode, MethodSignatures.BALANCE, accountsCreateOutput.getAccount()))
				.orElseThrow(() -> new IllegalStateException(MethodSignatures.BALANCE + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.BALANCE, IllegalStateException::new);

		// the balance in the account must be as required
		assertEquals(BigInteger.valueOf(12345), balance);

		String publicKeyBase64 = node.runInstanceMethodCallTransaction(
				TransactionRequests.instanceViewMethodCall(gamete, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, accountsCreateOutput.getAccount()))
				.orElseThrow(() -> new IllegalStateException(MethodSignatures.PUBLIC_KEY + " should not return void"))
				.asReturnedString(MethodSignatures.PUBLIC_KEY, IllegalStateException::new);

		var keyPairOfNewAccount = Entropies.load(accountsCreateOutput.getFile().get()).keys(passwordOfNewAccount, signature);
		byte[] encodingOfPublicKeyOfNewAccount = signature.encodingOf(keyPairOfNewAccount.getPublic());
		// the public key in the account must be as reported in the created key pair file
		assertEquals(Base64.toBase64String(encodingOfPublicKeyOfNewAccount), publicKeyBase64);
	}

	@Test
	@DisplayName("[moka accounts create] the creation of new account from another account works")
	public void accountCreationFromOtherAccountWorks() throws Exception {
		var signature = SignatureAlgorithms.ed25519();
		String passwordOfNewAccount = "abcde";

		// first we create a key pair
		var keyCreateOutputs = KeysCreateOutputs.from(MokaNew.keysCreate("--signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir));
		// then we create a new account with that key pair, and let the gamete for it
		var accountsCreateOutput = AccountsCreateOutputs.from(MokaNew.accountsCreate("12345 --payer=" + gamete + " --password-of-payer=" + passwordOfGamete + " --dir=" + dir + " --keys=" + keyCreateOutputs.getFile() + " --signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		TransactionResponse response = node.getResponse(accountsCreateOutput.getTransaction());

		// the response in the output of the command should be successful
		assertTrue(response instanceof ConstructorCallTransactionSuccessfulResponse);
		ConstructorCallTransactionSuccessfulResponse successfulResponse = (ConstructorCallTransactionSuccessfulResponse) response;

		// that response must have, as result, the created account
		assertEquals(accountsCreateOutput.getAccount(), successfulResponse.getNewObject());

		BigInteger balance = node.runInstanceMethodCallTransaction(
				TransactionRequests.instanceViewMethodCall(gamete, _100_000, takamakaCode, MethodSignatures.BALANCE, accountsCreateOutput.getAccount()))
				.orElseThrow(() -> new IllegalStateException(MethodSignatures.BALANCE + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.BALANCE, IllegalStateException::new);

		// the balance in the account must be as required
		assertEquals(BigInteger.valueOf(12345), balance);

		String publicKeyBase64 = node.runInstanceMethodCallTransaction(
				TransactionRequests.instanceViewMethodCall(gamete, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, accountsCreateOutput.getAccount()))
				.orElseThrow(() -> new IllegalStateException(MethodSignatures.PUBLIC_KEY + " should not return void"))
				.asReturnedString(MethodSignatures.PUBLIC_KEY, IllegalStateException::new);

		var keyPairOfNewAccount = Entropies.load(accountsCreateOutput.getFile().get()).keys(passwordOfNewAccount, signature);
		byte[] encodingOfPublicKeyOfNewAccount = signature.encodingOf(keyPairOfNewAccount.getPublic());
		// the public key in the account must be as reported in the created key pair file
		assertEquals(Base64.toBase64String(encodingOfPublicKeyOfNewAccount), publicKeyBase64);
	}

	@Test
	@DisplayName("[moka accounts show] the description of new account is correct")
	public void descriptionOfNewAccountIsCorrect() throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		String passwordOfNewAccount = "abcde";

		// first we create a key pair
		var keyCreateOutputs = KeysCreateOutputs.from(MokaNew.keysCreate("--signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir));
		// then we create a new account with that key pair, and let the faucet pay for it
		var accountsCreateOutput = AccountsCreateOutputs.from(MokaNew.accountsCreate("12345 --keys=" + keyCreateOutputs.getFile() + " --signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		// and finally ask to show the account
		var accountsShowOutput = AccountsShowOutputs.from(MokaNew.accountsShow(accountsCreateOutput.getAccount() + " --json --uri=ws://localhost:" + PORT));

		assertEquals(signature, accountsShowOutput.getSignature());
		assertEquals(BigInteger.valueOf(12345L), accountsShowOutput.getBalance());
		var keyPairOfNewAccount = Entropies.load(accountsCreateOutput.getFile().get()).keys(passwordOfNewAccount, signature);
		byte[] encodingOfPublicKeyOfNewAccount = signature.encodingOf(keyPairOfNewAccount.getPublic());
		assertEquals(Base64.toBase64String(encodingOfPublicKeyOfNewAccount), accountsShowOutput.getPublicKeyBase64());
		assertEquals(Base58.toBase58String(encodingOfPublicKeyOfNewAccount), accountsShowOutput.getPublicKeyBase58());
	}
}