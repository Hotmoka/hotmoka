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

package io.hotmoka.tools.internal.moka;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.Entropy;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.AccountCreationHelpers;
import io.hotmoka.helpers.api.AccountCreationHelper;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.api.Node;
import io.hotmoka.remote.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create-account",
	description = "Create a new account",
	showDefaultValues = true)
public class CreateAccount extends AbstractCommand {

	@Parameters(description = "the initial balance of the account", defaultValue = "0")
	private BigInteger balance;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = { "--payer" }, description = "the reference to the account that pays for the creation, or the string \"faucet\"", defaultValue = "faucet")
    private String payer;

	@Option(names = { "--key-of-new-account" }, description = "the Base58-encoded public key of the new account; if not specified, the password of the new account must be specified or it will be asked interactively")
    private String keyOfNewAccount;

	@Option(names = { "--password-of-new-account" }, description = "the password that will be used later to control the new account; if not specified and if no key is specified, it will be asked interactively")
    private String passwordOfNewAccount;

	@Option(names = { "--password-of-payer" }, description = "the password of the payer account, if it is not the faucet; if not specified, it will be asked interactively")
    private String passwordOfPayer;

	@Option(names = { "--balance-red" }, description = "the initial red balance of the account", defaultValue = "0")
    private BigInteger balanceRed;

	@Option(names = { "--signature" }, description = "the name of the signature algorithm to use for the new account {sha256dsa,ed25519,qtesla1,qtesla3,default}", defaultValue = "default")
	private String signature;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true")
	private boolean interactive;

	@Option(names = { "--create-tendermint-validator" }, description = "create a Tendermint ED25519 validator account (--signature will be ignored)")
	private boolean createTendermintValidator;

	@Option(names = { "--print-costs" }, description = "print the incurred gas costs", defaultValue = "true") 
	private boolean printCosts;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;
		private final PublicKey publicKey;
		private final AccountCreationHelper accountCreationHelper;
		private final SignatureAlgorithm signatureAlgorithmOfNewAccount;

		private Run() throws Exception {
			passwordOfPayer = ensurePassword(passwordOfPayer, "the payer account", interactive, "faucet".equals(payer));
			if (keyOfNewAccount != null)
				checkPublicKey(keyOfNewAccount);
			else
				passwordOfNewAccount = ensurePassword(passwordOfNewAccount, "the new account", interactive, false);
	
			if (createTendermintValidator)
				signature = "ed25519";

			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				String nameOfSignatureAlgorithmOfNewAccount = "default".equals(signature) ? node.getNameOfSignatureAlgorithmForRequests() : signature;
				signatureAlgorithmOfNewAccount = SignatureAlgorithms.of(nameOfSignatureAlgorithmOfNewAccount);

				Entropy entropy;
				if (keyOfNewAccount == null) {
					entropy = Entropies.random();
					publicKey = entropy.keys(passwordOfNewAccount, signatureAlgorithmOfNewAccount).getPublic();
				}
				else {
					entropy = Entropies.load(Paths.get(keyOfNewAccount + ".pem"));
					publicKey = signatureAlgorithmOfNewAccount.publicKeyFromEncoding(Base58.decode(keyOfNewAccount));
				}

				accountCreationHelper = AccountCreationHelpers.of(node);
				StorageReference accountReference = "faucet".equals(payer) ? createAccountFromFaucet() : createAccountFromPayer();
				var account = Accounts.of(entropy, accountReference);
	            System.out.println("A new account " + account + " has been created.");
	            Path fileName = account.dump();
	            System.out.println("Its entropy has been saved into the file \"" + fileName + "\".");
	            printPassphrase(account);
			}
		}

		private StorageReference createAccountFromFaucet() throws Exception {
			System.out.println("Free account creation will succeed only if the gamete of the node supports an open unsigned faucet.");
			
			try {
				if (createTendermintValidator)
					return accountCreationHelper.tendermintValidatorPaidByFaucet(publicKey, balance, balanceRed, this::printCosts);
				else
					return accountCreationHelper.paidByFaucet(signatureAlgorithmOfNewAccount, publicKey, balance,  balanceRed, this::printCosts);
			}
			catch (TransactionRejectedException e) {
				if (e.getMessage().contains("invalid request signature"))
					throw new IllegalStateException("invalid request signature: is the unsigned faucet of the node open?");

				throw e;
			}
		}

		private StorageReference createAccountFromPayer() throws Exception {
			checkStorageReference(payer);
			var payer = Accounts.of(CreateAccount.this.payer);
			KeyPair keysOfPayer = readKeys(payer, node, passwordOfPayer);
			if (createTendermintValidator)
				return accountCreationHelper.tendermintValidatorPaidBy
					(payer.getReference(), keysOfPayer, publicKey, balance, balanceRed, this::askForConfirmation, this::printCosts);
			else
				return accountCreationHelper.paidBy
					(payer.getReference(), keysOfPayer, signatureAlgorithmOfNewAccount, publicKey,
					balance, balanceRed, false, this::askForConfirmation, this::printCosts);
		}

		private void askForConfirmation(BigInteger gas) {
			if (interactive)
				yesNo("Do you really want to spend up to " + gas + " gas units to create a new account [Y/N] ");
		}

		private void printCosts(TransactionRequest<?>... requests) {
			if (printCosts)
				CreateAccount.this.printCosts(node, requests);
		}
	}
}