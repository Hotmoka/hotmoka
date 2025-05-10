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
import io.hotmoka.moka.AccountsRotateOutputs;
import io.hotmoka.moka.AccountsSendOutputs;
import io.hotmoka.moka.AccountsShowOutputs;
import io.hotmoka.moka.KeysBindOutputs;
import io.hotmoka.moka.KeysCreateOutputs;
import io.hotmoka.moka.Moka;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.NonVoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.values.StorageReference;

/**
 * Tests for the moka accounts commands.
 */
public class AccountsTests extends AbstractMokaTestWithNode {
	
	@Test
	@DisplayName("[moka accounts create] the creation of new account from the faucet works")
	public void accountCreationFromFaucetWorks() throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		String passwordOfNewAccount = "abcde";

		// first we create a key pair
		var keyCreateOutputs = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir));
		// then we create a new account with that key pair, and let the faucet pay for it
		var accountsCreateOutput = AccountsCreateOutputs.from(Moka.accountsCreate("12345 --keys=" + keyCreateOutputs.getFile() + " --signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		TransactionResponse response = node.getResponse(accountsCreateOutput.getTransaction());

		// the response in the output of the command should be successful
		assertTrue(response instanceof NonVoidMethodCallTransactionSuccessfulResponse);
		NonVoidMethodCallTransactionSuccessfulResponse successfulResponse = (NonVoidMethodCallTransactionSuccessfulResponse) response;

		// the created account is reported in the output
		assertTrue(accountsCreateOutput.getAccount().isPresent());
		StorageReference account = accountsCreateOutput.getAccount().get();

		// that response must have, as result, the created account
		assertEquals(account, successfulResponse.getResult());

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
	@DisplayName("[moka accounts create] the creation of new account from another account works")
	public void accountCreationFromOtherAccountWorks() throws Exception {
		var signature = SignatureAlgorithms.ed25519();
		String passwordOfNewAccount = "abcde";

		// first we create a key pair
		var keyCreateOutputs = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir));
		// then we create a new account with that key pair, and let the gamete pay for it
		var accountsCreateOutput = AccountsCreateOutputs.from(Moka.accountsCreate("12345 --payer=" + gamete + " --password-of-payer=" + passwordOfGamete + " --dir=" + dir + " --keys=" + keyCreateOutputs.getFile() + " --signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		TransactionResponse response = node.getResponse(accountsCreateOutput.getTransaction());

		// the response in the output of the command should be successful
		assertTrue(response instanceof ConstructorCallTransactionSuccessfulResponse);
		ConstructorCallTransactionSuccessfulResponse successfulResponse = (ConstructorCallTransactionSuccessfulResponse) response;

		// the created account is reported in the output
		assertTrue(accountsCreateOutput.getAccount().isPresent());
		StorageReference account = accountsCreateOutput.getAccount().get();

		// the new account must have been created in the reported transaction
		assertEquals(accountsCreateOutput.getTransaction(), account.getTransaction());

		// that response must have, as result, the created account
		assertEquals(account, successfulResponse.getNewObject());

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
	@DisplayName("[moka accounts show] the description of new account is correct")
	public void descriptionOfNewAccountIsCorrect() throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		String passwordOfNewAccount = "abcde";

		// first we create a key pair
		var keysCreateOutput = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir));
		// then we create a new account with that key pair, and let the faucet pay for it
		var accountsCreateOutput = AccountsCreateOutputs.from(Moka.accountsCreate("12345 --keys=" + keysCreateOutput.getFile() + " --signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		// and finally ask to show the account
		var accountsShowOutput = AccountsShowOutputs.from(Moka.accountsShow(accountsCreateOutput.getAccount().get() + " --json --uri=ws://localhost:" + PORT));

		assertEquals(signature, accountsShowOutput.getSignature());
		assertEquals(BigInteger.valueOf(12345L), accountsShowOutput.getBalance());
		var keyPairOfNewAccount = Entropies.load(accountsCreateOutput.getFile().get()).keys(passwordOfNewAccount, signature);
		byte[] encodingOfPublicKeyOfNewAccount = signature.encodingOf(keyPairOfNewAccount.getPublic());
		assertEquals(Base64.toBase64String(encodingOfPublicKeyOfNewAccount), accountsShowOutput.getPublicKeyBase64());
		assertEquals(Base58.toBase58String(encodingOfPublicKeyOfNewAccount), accountsShowOutput.getPublicKeyBase58());
	}

	@Test
	@DisplayName("[moka accounts rotate] the rotation of the keys of an account works")
	public void rotationOfKeysOfAccountWorks() throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		String passwordOfFirstKeyPair = "abcde";
		String passwordOfSecondKeyPair = "caramba";

		// create a first key pair: we provide explicit file names in order to avoid name clashes for long keys that start with the same prefix
		var firstKeyCreateOutput = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfFirstKeyPair + " --name first.pem --json --output-dir=" + dir));
		// create a second key pair
		var secondKeyCreateOutput = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfSecondKeyPair + " --name second.pem --json --output-dir=" + dir));
		// create a new account with the first key pair, letting the faucet pay for it
		var accountsCreateOutput = AccountsCreateOutputs.from(Moka.accountsCreate("50000000 --keys=" + firstKeyCreateOutput.getFile() + " --signature=" + signature + " --password=" + passwordOfFirstKeyPair + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		var account = accountsCreateOutput.getAccount().get();

		// we show the account
		var accountsShowOutput = AccountsShowOutputs.from(Moka.accountsShow(account + " --json --uri=ws://localhost:" + PORT));
		// the signature algorithm of the account is as required
		assertEquals(signature, accountsShowOutput.getSignature());
		// the public key of the account is that of the first key pair
		assertEquals(firstKeyCreateOutput.getPublicKeyBase64(), accountsShowOutput.getPublicKeyBase64());
		{
			// the public key in the key pair of the account is that of the first key pair
			var keyPairOfAccount = Entropies.load(accountsCreateOutput.getFile().get()).keys(passwordOfFirstKeyPair, signature);
			byte[] encodingOfPublicKeyOfAccount = signature.encodingOf(keyPairOfAccount.getPublic());
			assertEquals(Base64.toBase64String(encodingOfPublicKeyOfAccount), accountsShowOutput.getPublicKeyBase64());
		}
		// rotate the key pair of the account with the second key pair
		var accountsRotateOutput = AccountsRotateOutputs.from(Moka.accountsRotate(account + " --dir=" + dir + " --password-of-account=" + passwordOfFirstKeyPair + " --new-password-of-account=" + passwordOfSecondKeyPair + " --json --output-dir=" + dir + " --keys=" + secondKeyCreateOutput.getFile() + " --uri=ws://localhost:" + PORT));
		// we show the account again
		var accountsShowOutputAgain = AccountsShowOutputs.from(Moka.accountsShow(account + " --json --uri=ws://localhost:" + PORT));
		// the signature algorithm of the account is still as required
		assertEquals(signature, accountsShowOutputAgain.getSignature());
		// the public key of the account is that of the second key pair now
		assertEquals(secondKeyCreateOutput.getPublicKeyBase64(), accountsShowOutputAgain.getPublicKeyBase64());
		{
			// the public key in the new key pair of the account is that of the second key pair now
			var keyPairOfAccount = Entropies.load(accountsRotateOutput.getFile().get()).keys(passwordOfSecondKeyPair, signature);
			byte[] encodingOfPublicKeyOfAccount = signature.encodingOf(keyPairOfAccount.getPublic());
			assertEquals(Base64.toBase64String(encodingOfPublicKeyOfAccount), accountsShowOutputAgain.getPublicKeyBase64());
		}
	}

	@Test
	@DisplayName("[moka accounts send] sending coin from account to account works")
	public void sendFromAccountToAccountWorks() throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		String passwordOfDestination = "abcde";

		// create the key pair of the destination account: we provide explicit file names in order to avoid name clashes for long keys that start with the same prefix
		var destinationKeyCreateOutput = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfDestination + " --name destination.pem --json --output-dir=" + dir));
		// create a new account with that key pair, letting the faucet pay for it
		var accountsCreateOutput = AccountsCreateOutputs.from(Moka.accountsCreate("0 --keys=" + destinationKeyCreateOutput.getFile() + " --signature=" + signature + " --password=" + passwordOfDestination + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		var account = accountsCreateOutput.getAccount().get();
		// show the account
		var accountsShowOutput = AccountsShowOutputs.from(Moka.accountsShow(account + " --json --uri=ws://localhost:" + PORT));
		// the balance of the destination account is 0, currently
		assertEquals(BigInteger.ZERO, accountsShowOutput.getBalance());
		// send coins from the gamete to the new account
		var amount = BigInteger.valueOf(12345L);
		var accountsSendOutput = AccountsSendOutputs.from(Moka.accountsSend(gamete + " " + amount + " " + account + " --password-of-payer=" + passwordOfGamete + " --dir=" + dir + " --json --uri=ws://localhost:" + PORT));
		// there is no destination account from the accounts ledger, since we paid into a specific account, not into a key
		assertTrue(accountsSendOutput.getDestinationInAccountsLedger().isEmpty());
		// show the account again
		var accountsShowAgainOutput = AccountsShowOutputs.from(Moka.accountsShow(account + " --json --uri=ws://localhost:" + PORT));
		// the balance of the destination account has been increased as expected
		assertEquals(amount, accountsShowAgainOutput.getBalance());
	}

	@Test
	@DisplayName("[moka accounts send] sending coin from faucet to account works")
	public void sendFromFaucetToAccountWorks() throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		String passwordOfDestination = "abcde";

		// create the key pair of the destination account: we provide explicit file names in order to avoid name clashes for long keys that start with the same prefix
		var destinationKeyCreateOutput = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfDestination + " --name destination.pem --json --output-dir=" + dir));
		// create a new account with that key pair, letting the faucet pay for it
		var accountsCreateOutput = AccountsCreateOutputs.from(Moka.accountsCreate("0 --keys=" + destinationKeyCreateOutput.getFile() + " --signature=" + signature + " --password=" + passwordOfDestination + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		var account = accountsCreateOutput.getAccount().get();
		// show the account
		var accountsShowOutput = AccountsShowOutputs.from(Moka.accountsShow(account + " --json --uri=ws://localhost:" + PORT));
		// the balance of the destination account is 0, currently
		assertEquals(BigInteger.ZERO, accountsShowOutput.getBalance());
		// send coins from the faucet to the new account
		var amount = BigInteger.valueOf(12345L);
		var accountsSendOutput = AccountsSendOutputs.from(Moka.accountsSend("faucet " + amount + " " + account + " --json --uri=ws://localhost:" + PORT));
		// there is no destination account from the accounts ledger, since we paid into a specific account, not into a key
		assertTrue(accountsSendOutput.getDestinationInAccountsLedger().isEmpty());
		// show the account again
		var accountsShowAgainOutput = AccountsShowOutputs.from(Moka.accountsShow(account + " --json --uri=ws://localhost:" + PORT));
		// the balance of the destination account has been increased as expected
		assertEquals(amount, accountsShowAgainOutput.getBalance());
	}

	@Test
	@DisplayName("[moka accounts send] sending coin from account to public key works")
	public void sendFromAccountToPublicKeyWorks() throws Exception {
		var signature = SignatureAlgorithms.ed25519();
		String passwordOfDestination = "abcde";

		// create the key pair of the destination account: we provide explicit file names in order to avoid name clashes for long keys that start with the same prefix
		var keysCreateOutput = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfDestination + " --name destination.pem --json --output-dir=" + dir));
		var entropy = Entropies.load(keysCreateOutput.getFile());
		var keys = entropy.keys(passwordOfDestination, signature);
		String publicKeyBase58 = Base58.toBase58String(signature.encodingOf(keys.getPublic()));
		// send coins from the gamete to the public key
		var amount = BigInteger.valueOf(12345L);
		var accountsSendOutput = AccountsSendOutputs.from(Moka.accountsSend(gamete + " " + amount + " " + publicKeyBase58 + " --password-of-payer=" + passwordOfGamete + " --dir=" + dir + " --json --uri=ws://localhost:" + PORT));
		// there is a destination account from the accounts ledger, since we paid into a key, not into a specific account
		assertTrue(accountsSendOutput.getDestinationInAccountsLedger().isPresent());
		StorageReference account = accountsSendOutput.getDestinationInAccountsLedger().get();
		// show the account
		var accountsShowOutput = AccountsShowOutputs.from(Moka.accountsShow(account + " --json --uri=ws://localhost:" + PORT));
		// the balance of the destination account is as expected
		assertEquals(amount, accountsShowOutput.getBalance());
		// bind the account to the key pair
		var keysBindOutput = KeysBindOutputs.from(Moka.keysBind(keysCreateOutput.getFile() + " --password=" + passwordOfDestination + " --json --uri=ws://localhost:" + PORT));
		// the new pem file contains the same entropy as the original key pair
		var entropyBound = Entropies.load(keysBindOutput.getFile());
		assertEquals(entropy, entropyBound);
		
	}
}