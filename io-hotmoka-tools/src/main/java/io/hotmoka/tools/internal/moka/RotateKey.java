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
import java.util.Base64;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.Signers;
import io.hotmoka.crypto.api.Entropy;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.nodes.Accounts;
import io.hotmoka.nodes.api.Node;
import io.hotmoka.remote.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rotate-key",
	description = "Rotates the key of an account",
	showDefaultValues = true)
public class RotateKey extends AbstractCommand {

	@Parameters(index = "0", description = "the account whose key gets replaced")
    private String account;

	@Option(names = { "--password-of-account" }, description = "the password of the account; if not specified, it will be asked interactively")
	private String passwordOfAccount;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = "--classpath", description = "the classpath used to interpret the account", defaultValue = "the classpath of the account")
    private String classpath;

	@Option(names = { "--gas-price" }, description = "the gas price offered for the rotation", defaultValue = "the current price")
	private String gasPrice;

	@Option(names = { "--gas-limit" }, description = "the gas limit used for the rotation", defaultValue = "500000") 
	private BigInteger gasLimit;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true") 
	private boolean interactive;

	@Option(names = { "--print-costs" }, description = "print the incurred gas costs", defaultValue = "true") 
	private boolean printCosts;

	@Option(names = { "--use-colors" }, description = "use colors in the output", defaultValue = "true") 
	private boolean useColors;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;
		private final StorageReference account;
		private final TransactionReference classpath;
		private final InstanceMethodCallTransactionRequest request;
		private final Entropy entropy;

		private Run() throws Exception {
			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				if ("the classpath of the account".equals(RotateKey.this.classpath))
					this.classpath = node.getClassTag(new StorageReference(RotateKey.this.account)).jar;
				else
					this.classpath = new LocalTransactionReference(RotateKey.this.classpath);

				this.account = new StorageReference(RotateKey.this.account);
				passwordOfAccount = ensurePassword(passwordOfAccount, "the account", interactive, false);
				this.entropy = Entropies.random();
				
				askForConfirmation();
				this.request = createRequest();

				try {
					rotateKey();
					var rotatedAccount = Accounts.of(entropy, account);
		            System.out.println("The key of the account " + rotatedAccount + " has been rotated.");
		            String fileName = rotatedAccount.dump();
		            System.out.println("Its new entropy has been saved into the file \"" + fileName + "\".");
		            printPassphrase(rotatedAccount);
				}
				finally {
					if (printCosts)
						printCosts(node, request);
				}

			}
		}

		private InstanceMethodCallTransactionRequest createRequest() throws Exception {
			StorageReference manifest = node.getManifest();
			KeyPair keys = readKeys(Accounts.of(account), node, passwordOfAccount);
			String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, node.getTakamakaCode(), CodeSignature.GET_CHAIN_ID, manifest))).value;
			SignatureAlgorithm<SignedTransactionRequest> signature = SignatureHelpers.of(node).signatureAlgorithmFor(account);
			BigInteger nonce = NonceHelpers.of(node).getNonceOf(account);
			BigInteger gasPrice = getGasPrice();
			var signatureAlgorithmOfAccount = SignatureHelpers.of(node).signatureAlgorithmFor(account);
			PublicKey publicKey = entropy.keys(passwordOfAccount, signatureAlgorithmOfAccount).getPublic();
			String publicKeyEncoded = Base64.getEncoder().encodeToString(signatureAlgorithmOfAccount.encodingOf(publicKey));

			return new InstanceMethodCallTransactionRequest(
					Signers.with(signature, keys),
					account,
					nonce,
					chainId,
					gasLimit,
					gasPrice,
					classpath,
					new VoidMethodSignature(ClassType.EOA, "rotatePublicKey", ClassType.STRING),
					account,
					new StringValue(publicKeyEncoded));
		}

		private BigInteger getGasPrice() throws Exception {
			if ("the current price".equals(RotateKey.this.gasPrice))
				return GasHelpers.of(node).getGasPrice();
			else {
				BigInteger gasPrice;

				try {
					gasPrice = new BigInteger(RotateKey.this.gasPrice);
				}
				catch (NumberFormatException e) {
					throw new CommandException("The gas price must be a non-negative integer");
				}

				if (gasPrice.signum() < 0)
					throw new CommandException("The gas price must be non-negative");
				
				return gasPrice;
			}
		}

		private void rotateKey() throws Exception {
			node.addInstanceMethodCallTransaction(request);
		}

		private void askForConfirmation() throws ClassNotFoundException {
			if (interactive)
				yesNo("Do you really want to spend up to " + gasLimit + " gas units to rotate the key of account " + account + " ? [Y/N] ");
		}
	}
}