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

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.api.Entropy;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.PublicKeyIdentifier;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.node.remote.api.RemoteNode;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rotate",
	description = "Rotate the public key of an account.",
	showDefaultValues = true)
public class Rotate extends AbstractGasCostCommand {

	@Parameters(index = "0", description = "the account whose public key gets replaced", converter = StorageReferenceOfAccountOptionConverter.class)
    private StorageReference account;

	@ArgGroup(exclusive = true, multiplicity = "1", heading = "The new public key of the account must be specified in either of these two alternative ways:\n")
	private PublicKeyIdentifier publicKeyIdentifier;

	@Option(names = "--password", description = "the password of the new key pair of the account, if --keys is specified", interactive = true, defaultValue = "")
    private char[] password;

	@Option(names = "--payer", description = "the account that pays for rotating the key; if missing, it will be assumed to coincide with the account itself", converter = StorageReferenceOfAccountOptionConverter.class)
	private StorageReference payer;

	@Option(names = "--password-of-payer", description = "the password of the payer account, if --payer is specified", interactive = true, defaultValue = "")
    private char[] passwordOfPayer;

	@Option(names = "--dir", description = "the path of the directory where the current key pairs of the account and of the payer can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--output-dir", description = "the path of the directory where the new key pair of the account will be written", defaultValue = "")
	private Path outputDir;

	@Option(names = "--classpath", description = "the classpath used for the rotation transaction; if missing, the transaction that created the account will be used")
    private String classpath;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		new Run(remote);
	}

	private class Run {
		/*private final Node node;
		private final StorageReference account;
		private final TransactionReference classpath;
		private final InstanceMethodCallTransactionRequest request;
		private final Entropy entropy;*/

		private Run(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			/*this.account = StorageValues.reference(Rotate.this.account);

				if ("the classpath of the account".equals(Rotate.this.classpath))
					this.classpath = node.getClassTag(account).getJar();
				else
					this.classpath = TransactionReferences.of(Rotate.this.classpath);

				this.entropy = Entropies.random();
				
				askForConfirmation();
				this.request = createRequest();

				try {
					rotateKey();
					var rotatedAccount = Accounts.of(entropy, account);
		            System.out.println("The key of the account " + rotatedAccount + " has been rotated.");
		            System.out.println("Its new entropy has been saved into the file \"" + rotatedAccount.dump() + "\".");
				}
				finally {
					if (printCosts)
						{} //printCosts(node, request);
				}*/
		}

		/*
		private InstanceMethodCallTransactionRequest createRequest() throws Exception {
			var manifest = node.getManifest();
			var takamakaCode = node.getTakamakaCode();
			KeyPair keys = null; //readKeys(Accounts.of(account), node, passwordOfAccount);
			String chainId = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))
				.orElseThrow(() -> new CommandException(MethodSignatures.GET_CHAIN_ID + " should not return void"))).getValue();
			var signature = SignatureHelpers.of(node).signatureAlgorithmFor(account);
			BigInteger nonce = NonceHelpers.of(node).getNonceOf(account);
			BigInteger gasPrice = getGasPrice();
			var signatureAlgorithmOfAccount = SignatureHelpers.of(node).signatureAlgorithmFor(account);
			PublicKey publicKey = entropy.keys(passwordOfAccount, signatureAlgorithmOfAccount).getPublic();
			String publicKeyEncoded = Base64.toBase64String(signatureAlgorithmOfAccount.encodingOf(publicKey));

			return TransactionRequests.instanceMethodCall(
					signature.getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
					account,
					nonce,
					chainId,
					gasLimit,
					gasPrice,
					classpath,
					MethodSignatures.ofVoid(StorageTypes.EOA, "rotatePublicKey", StorageTypes.STRING),
					account,
					StorageValues.stringOf(publicKeyEncoded));
		}

		private BigInteger getGasPrice() throws Exception {
			if ("the current price".equals(Rotate.this.gasPrice))
				return GasHelpers.of(node).getGasPrice();
			else {
				BigInteger gasPrice;

				try {
					gasPrice = new BigInteger(Rotate.this.gasPrice);
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
		*/
	}
}