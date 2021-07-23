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
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import io.hotmoka.beans.SignatureAlgorithm;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNode;
import io.hotmoka.views.AccountCreationHelper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create-account",
	description = "Creates a new account",
	showDefaultValues = true)
public class CreateAccount extends AbstractCommand {

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = { "--payer" }, description = "the reference to the account that pays for the creation, or the string \"faucet\"", defaultValue = "faucet")
    private String payer;

	@Parameters(description = "the initial balance of the account", defaultValue = "0")
    private BigInteger balance;

	@Option(names = { "--balance-red" }, description = "the initial red balance of the account", defaultValue = "0")
    private BigInteger balanceRed;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode") 
	private boolean nonInteractive;

	@Option(names = { "--signature" }, description = "the name of the signature algorithm to use for the new account {sha256dsa,ed25519,qtesla1,qtesla3,default}", defaultValue = "default")
	private String signature;

	@Option(names = { "--public-key" }, description = "the Base64-encoded public key of the account that must be created; if not specified, a new key pair will be generated", defaultValue = "")
	private String publicKey;

	@Option(names = { "--ledger" }, description = "adds the newly created account to the ledger of the manifest, mapped to its public key")
	private boolean ledger;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;
		private final KeyPair keys;
		private final AccountCreationHelper accountCreationHelper;
		private final StorageReference account;
		private final SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithmOfNewAccount;
		private final String nameOfSignatureAlgorithmOfNewAccount;

		private Run() throws Exception {
			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				nameOfSignatureAlgorithmOfNewAccount = "default".equals(signature) ? node.getNameOfSignatureAlgorithmForRequests() : signature;
				signatureAlgorithmOfNewAccount = SignatureAlgorithmForTransactionRequests.mk(nameOfSignatureAlgorithmOfNewAccount);
				keys = publicKey.isBlank() ? signatureAlgorithmOfNewAccount.getKeyPair() : null;
				accountCreationHelper = new AccountCreationHelper(node);
				account = "faucet".equals(payer) ? createAccountFromFaucet() : createAccountFromPayer();
				System.out.println("A new account " + account + " has been created");

				if (publicKey.isBlank()) {
					dumpKeys(account, keys, node);
					System.out.println("The keys of the account have been saved into the files " + account + ".[pri|pub]");
				}
			}
		}

		private StorageReference createAccountFromFaucet() throws Exception {
			System.out.println("Free account creation will succeed only if the gamete of the node supports an open unsigned faucet");
			return accountCreationHelper.fromFaucet(signatureAlgorithmOfNewAccount, publicKey(), balance,  balanceRed, ledger, this::printCosts);
		}

		private StorageReference createAccountFromPayer() throws Exception {
			StorageReference payer = new StorageReference(CreateAccount.this.payer);
			KeyPair keysOfPayer = readKeys(payer, node);
			return accountCreationHelper.fromPayer(payer, keysOfPayer, signatureAlgorithmOfNewAccount, publicKey(), balance, balanceRed, ledger, this::askForConfirmation, this::printCosts);
		}

		private PublicKey publicKey() throws InvalidKeySpecException {
			if (CreateAccount.this.publicKey.isBlank())
				return keys.getPublic();
			else
				return signatureAlgorithmOfNewAccount.publicKeyFromEncoded(Base64.getDecoder().decode(CreateAccount.this.publicKey));
		}

		private void askForConfirmation(BigInteger gas) {
			if (!nonInteractive)
				yesNo("Do you really want to spend up to " + gas + " gas units to create a new account [Y/N] ");
		}

		private void printCosts(TransactionRequest<?>... requests) {
			CreateAccount.this.printCosts(node, requests);
		}
	}
}