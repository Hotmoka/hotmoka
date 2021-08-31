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

import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.Account;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNode;
import io.hotmoka.views.SendCoinsHelper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "send",
	description = "Sends units of coin to a payable contract",
	showDefaultValues = true)
public class Send extends AbstractCommand {

	@Parameters(index = "0", description = "the amount of coins sent to the contract")
    private BigInteger amount;

	@Parameters(index = "1", description = "the reference to the payable contract that receives the coins")
    private String contract;

	@Option(names = { "--payer" }, description = "the reference to the account that pays for the creation, or the string \"faucet\"", defaultValue = "faucet")
    private String payer;

	@Option(names = { "--password-of-payer" }, description = "the password of the payer account, if it is not the faucet; if not specified, it will be asked interactively")
    private String passwordOfPayer;

	@Option(names = { "--amount-red" }, description = "the amount of red coins sent to the contract", defaultValue = "0")
    private BigInteger amountRed;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode") 
	private boolean nonInteractive;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;
		private final StorageReference contract;

		private Run() throws Exception {
			contract = new StorageReference(Send.this.contract);

			if (passwordOfPayer != null && !nonInteractive)
				throw new IllegalArgumentException("the password of the payer account can be provided as command switch only in non-interactive mode");

			if (passwordOfPayer != null && "faucet".equals(payer))
				throw new IllegalArgumentException("the password of the payer has no meaning when the payer is the faucet");

			passwordOfPayer = ensurePassword(passwordOfPayer, "the payer account", nonInteractive, "faucet".equals(payer));

			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				if ("faucet".equals(payer))
					sendCoinsFromFaucet();
				else
					sendCoinsFromPayer();
			}
		}

		private void sendCoinsFromPayer() throws Exception {
			SendCoinsHelper sendCoinsHelper = new SendCoinsHelper(node);
			StorageReference payer = new StorageReference(Send.this.payer);
			KeyPair keysOfPayer = readKeys(new Account(payer), node, passwordOfPayer);
			sendCoinsHelper.fromPayer(payer, keysOfPayer, contract, amount, amountRed, this::askForConfirmation, this::printCosts);
		}

		private void sendCoinsFromFaucet() throws Exception {
			SendCoinsHelper sendCoinsHelper = new SendCoinsHelper(node);
			sendCoinsHelper.fromFaucet(contract, amount, amountRed, this::askForConfirmation, this::printCosts);
		}

		private void askForConfirmation(BigInteger gas) {
			if (!nonInteractive)
				yesNo("Do you really want to spend up to " + gas + " gas units to send the coins [Y/N] ");
		}

		private void printCosts(TransactionRequest<?>... requests) {
			Send.this.printCosts(node, requests);
		}
	}
}