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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.moka.AccountsRotateOutputs;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.api.accounts.AccountsRotateOutput;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.PublicKeyOrKeyPair;
import io.hotmoka.moka.internal.converters.PublicKeyOrKeyPairOptionConverter;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.json.AccountsRotateOutputJson;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.MisbehavingNodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnexpectedCodeException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rotate",
	description = "Rotate the public key of an account.",
	showDefaultValues = true)
public class Rotate extends AbstractGasCostCommand {

	@Parameters(index = "0", description = "the storage reference of the account whose public key gets rotated and that pays for the rotation", converter = StorageReferenceOptionConverter.class)
    private StorageReference account;

	@Parameters(index = "1", paramLabel = "<Base58-encoded public key or path>", description = "the new public key of the account, given either as an explicit Base58-encoded public key or as the path of a key pair file containing the public key", converter = PublicKeyOrKeyPairOptionConverter.class)
	private PublicKeyOrKeyPair newPublicKeyOrKeyPair;

	@Option(names = { "--password-of-account", "--password-of-payer" }, description = "the password of the current key pair of the account", interactive = true, defaultValue = "")
    private char[] passwordOfAccount;

	@Option(names = "--new-password-of-account", description = "the password of the new key pair of the account, only used if --keys is specified", interactive = true, defaultValue = "")
    private char[] newPasswordOfAccount;

	@Option(names = "--dir", paramLabel = "<path>", description = "the directory where the current key pair of the account can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--output-dir", paramLabel = "<path>", description = "the directory where the new key pair of the account will be written", defaultValue = "")
	private Path outputDir;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, UninitializedNodeException, ClosedNodeException, MisbehavingNodeException, UnexpectedCodeException {
		new Body(remote);
	}

	private class Body {
		private final String chainId;
		private final Signer<SignedTransactionRequest<?>> signer;
		private final BigInteger gasLimit;
		private final BigInteger gasPrice;
		private final TransactionReference classpath;
		private final String newPublicKeyBase64;
		private final BigInteger nonce;
		private final InstanceMethodCallTransactionRequest request;

		private Body(RemoteNode remote) throws TimeoutException, InterruptedException, ClosedNodeException, CommandException, UninitializedNodeException, MisbehavingNodeException, UnexpectedCodeException {
			String passwordOfAccountAsString = new String(passwordOfAccount);
			String newPasswordOfAccountAsString = new String(newPasswordOfAccount);

			try {
				this.chainId = remote.getConfig().getChainId();
				SignatureAlgorithm signatureOfAccount = determineSignatureOf(account, remote);
				this.signer = mkSigner(account, dir, signatureOfAccount, passwordOfAccountAsString);
				this.gasLimit = determineGasLimit(() -> gasForTransactionWhosePayerHasSignature(signatureOfAccount));
				this.gasPrice = determineGasPrice(remote);
				this.classpath = getClasspathAtCreationTimeOf(account, remote);
				this.newPublicKeyBase64 = newPublicKeyOrKeyPair.getPublicKeyBase64(signatureOfAccount, newPasswordOfAccountAsString);
				askForConfirmation("rotate the public key of " + account, gasLimit, gasPrice, yes || json());
				this.nonce = determineNonceOf(account, remote);
				this.request = mkRequest();
				report(json(), executeRequest(remote), AccountsRotateOutputs.Encoder::new);
			}
			finally {
				passwordOfAccountAsString = null;
				newPasswordOfAccountAsString = null;
				Arrays.fill(passwordOfAccount, ' ');
				Arrays.fill(newPasswordOfAccount, ' ');
			}
		}

		private Output executeRequest(RemoteNode remote) throws CommandException, ClosedNodeException, TimeoutException, InterruptedException {
			TransactionReference transaction = computeTransaction(request);
			Optional<GasCost> gasCost = Optional.empty();
			Optional<String> errorMessage = Optional.empty();
			Optional<Path> file = Optional.empty();
			Optional<StorageReference> account = Optional.empty();

			try {
				if (post()) {
					if (!json())
						System.out.print("Posting transaction " + asTransactionReference(transaction) + "... ");

					remote.postInstanceMethodCallTransaction(request);
					if (!json())
						System.out.println("done.");
				}
				else {
					if (!json())
						System.out.print("Adding transaction " + asTransactionReference(transaction) + "... ");

					try {
						remote.addInstanceMethodCallTransaction(request);
						if (!json())
							System.out.println("done.");

						account = Optional.of(Rotate.this.account);
						file = bindKeysToAccount(newPublicKeyOrKeyPair, account.get(), outputDir);
						gasCost = Optional.of(computeIncurredGasCost(remote, gasPrice, transaction));
					}
					catch (TransactionException | CodeExecutionException e) {
						if (!json())
							System.out.println("failed.");

						errorMessage = Optional.of(e.getMessage());
						gasCost = Optional.of(computeIncurredGasCost(remote, gasPrice, transaction));
					}
				}
			}
			catch (TransactionRejectedException e) {
				if (!json())
					System.out.println("rejected.");

				errorMessage = Optional.of(e.getMessage());
			}

			return new Output(transaction, account, gasCost, errorMessage, file);
		}

		private InstanceMethodCallTransactionRequest mkRequest() throws CommandException {
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

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractGasCostCommandOutput implements AccountsRotateOutput {

		/**
		 * The account whose keys have been rotated, if any.
		 */
		private final Optional<StorageReference> account;

		/**
		 * The path where the new key pair of the account has been saved, if any.
		 */
		private final Optional<Path> file;
	
		/**
		 * Builds the output of the command.
		 */
		private Output(TransactionReference transaction, Optional<StorageReference> account, Optional<GasCost> gasCost, Optional<String> errorMessage, Optional<Path> file) {
			super(transaction, gasCost, errorMessage);

			this.account = account;
			this.file = file;
		}
	
		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(AccountsRotateOutputJson json) throws InconsistentJsonException {
			super(json);

			var account = json.getAccount();
			if (account.isEmpty())
				this.account = Optional.empty();
			else
				this.account = Optional.of(account.get().unmap().asReference(value -> new InconsistentJsonException("The reference to the account must be a storage reference, not a " + value.getClass().getName())));

			Optional<String> file = json.getFile();
			if (file.isPresent()) {
				try {
					this.file = Optional.of(Paths.get(file.get()));
				}
				catch (InvalidPathException e) {
					throw new InconsistentJsonException(e);
				}
			}
			else
				this.file = Optional.empty();
		}

		@Override
		public Optional<StorageReference> getAccount() {
			return account;
		}

		@Override
		public Optional<Path> getFile() {
			return file;
		}

		@Override
		protected void toString(StringBuilder sb) {
			sb.append("The keys of the account " + account + " have been rotated.\n");
		}
	}
}