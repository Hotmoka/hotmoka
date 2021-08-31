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
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.atomic.AtomicInteger;

import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.Account;
import io.hotmoka.crypto.BIP39Dictionary;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.SignatureAlgorithm;
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

	@Option(names = { "--password-of-new-account" }, description = "the password that will be used later to control the new account; if not specified, it will be asked interactively")
    private String passwordOfNewAccount;

	@Option(names = { "--password-of-payer" }, description = "the password of the payer account, if it is not the faucet; if not specified, it will be asked interactively")
    private String passwordOfPayer;

	@Parameters(description = "the initial balance of the account", defaultValue = "0")
    private BigInteger balance;

	@Option(names = { "--balance-red" }, description = "the initial red balance of the account", defaultValue = "0")
    private BigInteger balanceRed;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode")
	private boolean nonInteractive;

	@Option(names = { "--signature" }, description = "the name of the signature algorithm to use for the new account {sha256dsa,ed25519,qtesla1,qtesla3,default}", defaultValue = "default")
	private String signature;

	@Option(names = { "--public-key" }, description = "the Base58-encoded public key of the account that must be created; if not specified, a new key pair will be generated")
	private String publicKey;

	@Option(names = { "--ledger" }, description = "adds the newly created account to the ledger of the manifest, mapped to its public key; only available if a public key is explicitly specified")
	private boolean ledger;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private boolean publicKeySpecified() {
		return publicKey != null && !publicKey.isBlank();
	}

	private class Run {
		private final Node node;
		private final byte[] entropy = new byte[16];
		private final KeyPair keys;
		private final AccountCreationHelper accountCreationHelper;
		private final SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithmOfNewAccount;

		private Run() throws Exception {
			if (ledger && "faucet".equals(payer))
				throw new IllegalArgumentException("you cannot store in the ledger accounts created from the faucet");

			if (ledger && !publicKeySpecified())
				throw new IllegalArgumentException("you can only store in the ledger accounts for user-provided public keys");

			if (passwordOfNewAccount != null && !nonInteractive)
				throw new IllegalArgumentException("the password of the new account can be provided as command switch only in non-interactive mode");

			if (passwordOfPayer != null && !nonInteractive)
				throw new IllegalArgumentException("the password of the payer account can be provided as command switch only in non-interactive mode");

			if (passwordOfPayer != null && "faucet".equals(payer))
				throw new IllegalArgumentException("the password of the payer has no meaning when the payer is the faucet");

			if (passwordOfNewAccount != null && publicKeySpecified())
				throw new IllegalArgumentException("the password of the new account cannot be specified when paying to a public key");

			if (passwordOfNewAccount == null)
				if (publicKeySpecified())
					passwordOfNewAccount = ""; // unused
				else if (nonInteractive) {
					System.out.println("Using the empty string as password of the new account");
					passwordOfNewAccount = "";
				}
				else
					passwordOfNewAccount = askForPassword("Please specify the password of the new account: ");

			if (passwordOfPayer == null && !"faucet".equals(payer))
				if (nonInteractive) {
					System.out.println("Using the empty string as password of the payer account");
					passwordOfPayer = "";
				}
				else
					passwordOfPayer = askForPassword("Please specify the password of the payer account: ");

			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				String nameOfSignatureAlgorithmOfNewAccount = "default".equals(signature) ? node.getNameOfSignatureAlgorithmForRequests() : signature;
				signatureAlgorithmOfNewAccount = SignatureAlgorithmForTransactionRequests.mk(nameOfSignatureAlgorithmOfNewAccount);
				keys = publicKeySpecified() ? null : createNewKeyPair();
				accountCreationHelper = new AccountCreationHelper(node);
				StorageReference accountReference = "faucet".equals(payer) ? createAccountFromFaucet() : createAccountFromPayer();
				// currently, the progressive number of the created accounts will be #0,
	            // but we better check against future unexpected changes in the server's behavior
	            if (accountReference.progressive.signum() != 0)
	                throw new IllegalStateException("I can only deal with new accounts whose progressive number is 0.");

	            System.out.println("A new account " + accountReference + " has been created.");

	            if (!publicKeySpecified()) {
	            	Account account = new Account(entropy, accountReference);
					account.dump();
					System.out.println("The entropy of the account has been saved into the file " + account + ".pem.");
					System.out.println("Please take note of the following passphrase of 36 words,");
					System.out.println("you will need it to reinstall the account in this or another machine or application in the future:\n");
					AtomicInteger counter = new AtomicInteger(0);
					account.bip39Words().stream().forEachOrdered(word -> System.out.printf("%2d: %s\n", counter.incrementAndGet(), word));
				}
	            else if (ledger)
	            	System.out.println("The owner of the key can now see the new account for the key");
	            else
	            	System.out.println("The owner of the key can now associate the address of the new account to the key");
			}
		}

		private KeyPair createNewKeyPair() {
			SecureRandom random = new SecureRandom();
			random.nextBytes(entropy);
			return signatureAlgorithmOfNewAccount.getKeyPair(entropy, BIP39Dictionary.ENGLISH_DICTIONARY, passwordOfNewAccount);
		}

		private StorageReference createAccountFromFaucet() throws Exception {
			System.out.println("Free account creation will succeed only if the gamete of the node supports an open unsigned faucet.");
			return accountCreationHelper.fromFaucet(signatureAlgorithmOfNewAccount, publicKey(), balance,  balanceRed, this::printCosts);
		}

		private StorageReference createAccountFromPayer() throws Exception {
			Account payer = new Account(CreateAccount.this.payer);
			KeyPair keysOfPayer = readKeys(payer, node, passwordOfPayer);
			return accountCreationHelper.fromPayer
				(payer.reference, keysOfPayer, signatureAlgorithmOfNewAccount, publicKey(),
				balance, balanceRed, ledger, this::askForConfirmation, this::printCosts);
		}

		private PublicKey publicKey() throws InvalidKeySpecException {
			if (publicKeySpecified())
				return signatureAlgorithmOfNewAccount.publicKeyFromEncoding(Base58.decode(publicKey));
			else
				return keys.getPublic();
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