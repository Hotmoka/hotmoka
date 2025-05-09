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

package io.hotmoka.moka.internal.accounts;

import java.math.BigInteger;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.converters.Base58OptionConverter;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "send",
	description = "Send units of coin to a payable contract",
	showDefaultValues = true)
public class Send extends AbstractGasCostCommand {

	@Parameters(index = "0", description = "the amount of coins to send to the destination; this will be deduced from the balance of the payer", defaultValue = "0")
    private BigInteger amount;

	@ArgGroup(exclusive = true, multiplicity = "1", heading = "The destination must be specified in either of these two alternative ways:\n")
	private DestinationIdentifier destination;

	@Option(names = "--payer", paramLabel = "<storage reference>", description = "the account that pays for the amount sent to the destination; if missing, the faucet of the network will be used, if it is open", converter = StorageReferenceOptionConverter.class)
	private StorageReference payer;

	@Option(names = "--password-of-payer", description = "the password of the payer; this is not used if the payer is the faucet", interactive = true, defaultValue = "")
	private char[] passwordOfPayer;

	/**
	 * The specification of the destination of a send operation: either
	 * a Base58-encoded public key or a storage reference.
	 */
	private static class DestinationIdentifier {

		@Option(names = "--key", paramLabel = "<Base58-encoded string>", description = "as a public key", converter = Base58OptionConverter.class)
		private String key;

		@Option(names = "--account", paramLabel = "<storage reference>", description = "as an account", converter = StorageReferenceOptionConverter.class)
		private StorageReference account;
	}

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		new Body(remote);
	}

	private class Body {
		private final RemoteNode remote;

		private Body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			this.remote = remote;

			/*if (anonymous && "faucet".equals(payer))
				throw new IllegalArgumentException("you cannot send coins anonymously from the faucet");

			if (anonymous && !looksLikePublicKey(destination))
				throw new IllegalArgumentException("you can only send coins anonymously to a key");

			if (passwordOfPayer != null)
				throw new IllegalArgumentException("the password of the payer account can be provided as command switch only in non-interactive mode");

			if (passwordOfPayer != null && "faucet".equals(payer))
				throw new IllegalArgumentException("the password of the payer has no meaning when the payer is the faucet");

			try (var node = this.node = RemoteNodes.of(uri, 10_000)) {
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
			}*/
		}

		/*private void sendCoinsFromPayer() throws Exception {
			var sendCoinsHelper = SendCoinsHelpers.of(node);
			var payer = StorageValues.reference(Send.this.payer);
			KeyPair keysOfPayer = readKeys(Accounts.of(payer), node, passwordOfPayer);
			sendCoinsHelper.sendFromPayer(payer, keysOfPayer, StorageValues.reference(destination), amount, this::askForConfirmation, this::printCosts);
		}

		private StorageReference sendCoinsToPublicKey() throws Exception {
			var accountCreationHelper = AccountCreationHelpers.of(node);
			var payer = StorageValues.reference(Send.this.payer);
			KeyPair keysOfPayer = readKeys(Accounts.of(payer), node, passwordOfPayer);
			var signatureAlgorithmForNewAccount = SignatureAlgorithms.ed25519();
			return accountCreationHelper.paidBy(payer, keysOfPayer, signatureAlgorithmForNewAccount,
				signatureAlgorithmForNewAccount.publicKeyFromEncoding(Base58.fromBase58String(destination)),
				amount, anonymous, this::askForConfirmation, this::printCosts);
		}

		private void sendCoinsFromFaucet() throws Exception {
			var sendCoinsHelper = SendCoinsHelpers.of(node);
			
			try {
				sendCoinsHelper.sendFromFaucet(StorageValues.reference(destination), amount, this::askForConfirmation, this::printCosts);
			}
			catch (TransactionRejectedException e) {
				if (e.getMessage().contains("invalid request signature"))
					throw new IllegalStateException("invalid request signature: is the unsigned faucet of the node open?");

				throw e;
			}
		}

		private void askForConfirmation(BigInteger gas) {
			if (!"faucet".equals(payer))
				yesNo("Do you really want to spend up to " + gas + " gas units to send the coins [Y/N] ");
		}

		private void printCosts(TransactionRequest<?>... requests) {
		}*/
	}
}