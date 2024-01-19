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
import java.security.KeyPair;

import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.helpers.AccountCreationHelpers;
import io.hotmoka.helpers.SendCoinsHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.remote.RemoteNodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "send",
	description = "Send units of coin to a payable contract",
	showDefaultValues = true)
public class Send extends AbstractCommand {

	@Parameters(index = "0", description = "the amount of coins sent to the contract")
    private BigInteger amount;

	@Parameters(index = "1", description = "the reference to the payable contract or the Base58-encoded key that receives the coins")
    private String destination;

	@Option(names = { "--payer" }, description = "the reference to the account that pays for the creation, or the string \"faucet\"", defaultValue = "faucet")
    private String payer;

	@Option(names = { "--password-of-payer" }, description = "the password of the payer account, if it is not the faucet; if not specified, it will be asked interactively")
    private String passwordOfPayer;

	@Option(names = { "--amount-red" }, description = "the amount of red coins sent to the contract", defaultValue = "0")
    private BigInteger amountRed;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = { "--anonymous" }, description = "send coins anonymously to a key")
	private boolean anonymous;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true") 
	private boolean interactive;

	@Option(names = { "--print-costs" }, description = "print the incurred gas costs", defaultValue = "true") 
	private boolean printCosts;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;

		private Run() throws Exception {
			if (anonymous && "faucet".equals(payer))
				throw new IllegalArgumentException("you cannot send coins anonymously from the faucet");

			if (anonymous && !looksLikePublicKey(destination))
				throw new IllegalArgumentException("you can only send coins anonymously to a key");

			if (passwordOfPayer != null && interactive)
				throw new IllegalArgumentException("the password of the payer account can be provided as command switch only in non-interactive mode");

			if (passwordOfPayer != null && "faucet".equals(payer))
				throw new IllegalArgumentException("the password of the payer has no meaning when the payer is the faucet");

			passwordOfPayer = ensurePassword(passwordOfPayer, "the payer account", interactive, "faucet".equals(payer));

			try (var node = this.node = RemoteNodes.of(remoteNodeConfig(url))) {
				if ("faucet".equals(payer))
					sendCoinsFromFaucet();
				else if (looksLikePublicKey(destination)) {
					StorageReference result = sendCoinsToPublicKey();
					
					if (anonymous)
		            	System.out.println("The owner of the key can now see the new account associated to the key.");
		            else
		            	System.out.println("The owner of the key can now associate the address " + result + " of the account to the key.");
				}
				else if (looksLikeStorageReference(destination))
					sendCoinsFromPayer();
				else
					throw new IllegalArgumentException("The destination does not look like a storage reference or a Base58-encoded key.");
			}
		}

		private void sendCoinsFromPayer() throws Exception {
			var sendCoinsHelper = SendCoinsHelpers.of(node);
			var payer = StorageValues.reference(Send.this.payer);
			KeyPair keysOfPayer = readKeys(Accounts.of(payer), node, passwordOfPayer);
			sendCoinsHelper.sendFromPayer(payer, keysOfPayer, StorageValues.reference(destination), amount, amountRed, this::askForConfirmation, this::printCosts);
		}

		private StorageReference sendCoinsToPublicKey() throws Exception {
			var accountCreationHelper = AccountCreationHelpers.of(node);
			var payer = StorageValues.reference(Send.this.payer);
			KeyPair keysOfPayer = readKeys(Accounts.of(payer), node, passwordOfPayer);
			var signatureAlgorithmForNewAccount = SignatureAlgorithms.ed25519();
			return accountCreationHelper.paidBy(payer, keysOfPayer, signatureAlgorithmForNewAccount,
				signatureAlgorithmForNewAccount.publicKeyFromEncoding(Base58.decode(destination)),
				amount, amountRed, anonymous, this::askForConfirmation, this::printCosts);
		}

		private void sendCoinsFromFaucet() throws Exception {
			var sendCoinsHelper = SendCoinsHelpers.of(node);
			
			try {
				sendCoinsHelper.sendFromFaucet(StorageValues.reference(destination), amount, amountRed, this::askForConfirmation, this::printCosts);
			}
			catch (TransactionRejectedException e) {
				if (e.getMessage().contains("invalid request signature"))
					throw new IllegalStateException("invalid request signature: is the unsigned faucet of the node open?");

				throw e;
			}
		}

		private void askForConfirmation(BigInteger gas) {
			if (interactive && !"faucet".equals(payer))
				yesNo("Do you really want to spend up to " + gas + " gas units to send the coins [Y/N] ");
		}

		private void printCosts(TransactionRequest<?>... requests) {
			if (printCosts)
				Send.this.printCosts(node, requests);
		}
	}
}