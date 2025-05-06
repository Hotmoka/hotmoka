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
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.api.GasCost;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.PublicKeyIdentifier;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rotate",
	description = "Rotate the public key of an account.",
	showDefaultValues = true)
public class Rotate extends AbstractGasCostCommand {

	@Parameters(index = "0", description = "the account whose public key gets rotated and that pays for the rotation", converter = StorageReferenceOfAccountOptionConverter.class)
    private StorageReference account;

	@Option(names = { "--password-of-account", "--password-of-payer" }, description = "the password of the current key pair of the account", interactive = true, defaultValue = "")
    private char[] passwordOfAccount;

	@Option(names = "--new-password-of-account", description = "the password of the new key pair of the account, only used if --keys is specified", interactive = true, defaultValue = "")
    private char[] newPasswordOfAccount;

	@ArgGroup(exclusive = true, multiplicity = "1", heading = "The new public key of the account must be specified in either of these two alternative ways:\n")
	private PublicKeyIdentifier newPublicKeyIdentifier;

	@Option(names = "--dir", description = "the path of the directory where the current key pair of the account can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--output-dir", description = "the path of the directory where the new key pair of the account will be written", defaultValue = "")
	private Path outputDir;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		new Run(remote);
	}

	private class Run {
		private final String chainId;
		private final Signer<SignedTransactionRequest<?>> signer;
		private final BigInteger gasLimit;
		private final BigInteger gasPrice;
		private final TransactionReference classpath;
		private final String newPublicKeyBase64;
		private final BigInteger nonce;
		private final InstanceMethodCallTransactionRequest request;

		private Run(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			String passwordOfAccountAsString = new String(passwordOfAccount);
			String newPasswordOfAccountAsString = new String(newPasswordOfAccount);

			try {
				this.chainId = remote.getConfig().getChainId();
				SignatureAlgorithm signatureOfAccount = determineSignatureOf(account, remote);
				this.signer = mkSigner(account, dir, signatureOfAccount, passwordOfAccountAsString);
				this.gasLimit = determineGasLimit(() -> gasForTransactionWhosePayerHasSignature(signatureOfAccount));
				this.gasPrice = determineGasPrice(remote);
				this.classpath = getClasspathAtCreationTimeOf(account, remote);
				this.newPublicKeyBase64 = newPublicKeyIdentifier.getPublicKeyBase64(signatureOfAccount, newPasswordOfAccountAsString);
				askForConfirmation("rotate the public key of " + account, gasLimit, gasPrice, yes || json());
				this.nonce = determineNonceOf(account, remote);
				this.request = mkRequest();
				executeRequest(remote);
				GasCost gasCost = computeIncurredGasCost(remote, request);
				bindKeysToAccount(newPublicKeyIdentifier, account, outputDir).ifPresent(path -> System.out.println("The new key pair of " + account + " has been saved as " + asPath(path)));
				System.out.println(gasCost);
			}
			finally {
				passwordOfAccountAsString = null;
				newPasswordOfAccountAsString = null;
				Arrays.fill(passwordOfAccount, ' ');
				Arrays.fill(newPasswordOfAccount, ' ');
			}
		}

		protected void executeRequest(RemoteNode remote) throws NodeException, TimeoutException, InterruptedException, CommandException {
			try {
				remote.addInstanceMethodCallTransaction(request);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("The public key rotation transaction failed! are the key pair of the account and its password correct?", e);
			}
		}

		protected InstanceMethodCallTransactionRequest mkRequest() throws CommandException {
			try {
				return TransactionRequests.instanceMethodCall(
						signer,
						account,
						nonce,
						chainId,
						gasLimit,
						gasPrice,
						classpath,
						MethodSignatures.ROTATE_PUBLIC_KEY,
						account,
						StorageValues.stringOf(newPublicKeyBase64));
			}
			catch (InvalidKeyException | SignatureException e) {
				throw new CommandException("The current key pair of " + account + " seems corrupted!", e);
			}
		}
	}
}