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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.helpers.UnexpectedVoidMethodException;
import io.hotmoka.moka.AccountsSendOutputs;
import io.hotmoka.moka.KeysBindOutputs;
import io.hotmoka.moka.KeysCreateOutputs;
import io.hotmoka.moka.Moka;
import io.hotmoka.moka.NodesTendermintValidatorsBuyOutputs;
import io.hotmoka.moka.NodesTendermintValidatorsCreateOutputs;
import io.hotmoka.moka.NodesTendermintValidatorsSellOutputs;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
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
		var accountsCreateOutput = NodesTendermintValidatorsCreateOutputs.from(Moka.nodesTendermintValidatorsCreate("faucet 12345 " + keyCreateOutputs.getFile() + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
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
		var accountsCreateOutput = NodesTendermintValidatorsCreateOutputs.from(Moka.nodesTendermintValidatorsCreate(gamete + " 12345 " + keyCreateOutputs.getFile() + " --password-of-payer=" + passwordOfGamete + " --dir=" + dir + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
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

	@Test
	@DisplayName("[moka nodes tendermint validators sell/buy] sale and buy of an offer of validation power works")
	public void saleBuyOfferOfValidationPowerWorks(@TempDir Path dir) throws Exception {
		// this test can only work for nodes that actually have at least a validator
		Optional<StorageReference> maybeValidator = getFirstValidator();
		if (maybeValidator.isPresent()) {
			var seller = maybeValidator.get();
			// create a new key pair
			var keysCreateOutput = KeysCreateOutputs.from(Moka.keysCreate("--name=buyer.pem --json --output-dir=" + dir));
			// create the buyer validator
			var nodesTendermintValidatorsCreateOutput = NodesTendermintValidatorsCreateOutputs.from(Moka.nodesTendermintValidatorsCreate("faucet 0 " + keysCreateOutput.getFile() + " --json --dir=" + dir + " --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
			// the account has been really created
			assertTrue(nodesTendermintValidatorsCreateOutput.getAccount().isPresent());
			var buyer = nodesTendermintValidatorsCreateOutput.getAccount().get();
			// the buyer is not a validator at the moment
			assertFalse(isValidator(buyer));
			// bind the key pair of the validator to the validator object
			KeysBindOutputs.from(Moka.keysBind(Paths.get("src", "test", "resources", "tendermint_configs", "v1n0", "node0", "validator.pem") + " --reference=" + seller + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
			// make the seller rich enough to pay for the next transactions
			AccountsSendOutputs.from(Moka.accountsSend("faucet 10000000 " + seller + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));
			// make the buyer rich enough to pay for the next transactions
			AccountsSendOutputs.from(Moka.accountsSend("faucet 10000000 " + buyer + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));
			// place a sale offer of 50 units of validation power for a price of 1000 coins
			var nodesTendermintValidatorsSellOutput = NodesTendermintValidatorsSellOutputs.from(Moka.nodesTendermintValidatorsSell(seller + " 50 1000 1000000 --json --dir=" + dir + " --uri=ws://localhost:" + PORT));
			// the offer has been really created
			assertTrue(nodesTendermintValidatorsSellOutput.getOffer().isPresent());
			var offer = nodesTendermintValidatorsSellOutput.getOffer().get();
			// read the shares on sale by the seller validator
			var sharesOnSaleBySeller = sharesOnSaleBy(seller);
			// the shares on sale are as many as expected
			assertEquals(BigInteger.valueOf(50), sharesOnSaleBySeller);
			// let the buyer accept the sale offer of validation power
			var nodesTendermintValidatorsBuy = NodesTendermintValidatorsBuyOutputs.from(Moka.nodesTendermintValidatorsBuy(buyer + " " + offer + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));
			// the sale has been successfully executed
			assertTrue(nodesTendermintValidatorsBuy.getErrorMessage().isEmpty());
			// the buyer is a validator now
			assertTrue(isValidator(buyer));
			// the seller validator is still a validator
			assertTrue(isValidator(seller));
		}
	}

	private BigInteger sharesOnSaleBy(StorageReference seller) throws Exception {
		var manifest = node.getManifest();
		var validators = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest))
				.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_VALIDATORS))
				.asReturnedReference(MethodSignatures.GET_VALIDATORS, UnexpectedValueException::new);
		var sharesOnSale = MethodSignatures.ofNonVoid(StorageTypes.classNamed("io.takamaka.code.dao.SimpleSharedEntity"), "sharesOnSaleOf", StorageTypes.BIG_INTEGER, StorageTypes.PAYABLE_CONTRACT);
		return node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, sharesOnSale, validators, seller))
				.orElseThrow(() -> new UnexpectedVoidMethodException(sharesOnSale))
				.asReturnedBigInteger(sharesOnSale, UnexpectedValueException::new);
	}

	private boolean isValidator(StorageReference validator) throws Exception {
		var manifest = node.getManifest();
		var validators = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest))
				.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_VALIDATORS))
				.asReturnedReference(MethodSignatures.GET_VALIDATORS, UnexpectedValueException::new);
		var isShareholder = MethodSignatures.ofNonVoid(StorageTypes.classNamed("io.takamaka.code.dao.SimpleSharedEntity"), "isShareholder", StorageTypes.BOOLEAN, StorageTypes.OBJECT);
		return node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, isShareholder, validators, validator))
				.orElseThrow(() -> new UnexpectedVoidMethodException(isShareholder))
				.asReturnedBoolean(isShareholder, UnexpectedValueException::new);
	}

	private Optional<StorageReference> getFirstValidator() throws Exception {
		var manifest = node.getManifest();
		var validators = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest))
				.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_VALIDATORS))
				.asReturnedReference(MethodSignatures.GET_VALIDATORS, UnexpectedValueException::new);
		var shares = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_SHARES, validators))
				.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_SHARES))
				.asReturnedReference(MethodSignatures.GET_SHARES, UnexpectedValueException::new);
		int numOfValidators = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SIZE, shares))
				.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.STORAGE_MAP_VIEW_SIZE))
				.asReturnedInt(MethodSignatures.STORAGE_MAP_VIEW_SIZE, UnexpectedValueException::new);

		if (numOfValidators > 0)
			return Optional.of(node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SELECT, shares, StorageValues.intOf(0)))
					.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.STORAGE_MAP_VIEW_SELECT))
					.asReturnedReference(MethodSignatures.STORAGE_MAP_VIEW_SELECT, UnexpectedValueException::new));
		else
			return Optional.empty();
	}
}