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

package io.hotmoka.moka.internal.keys;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.KeysBindOutputs;
import io.hotmoka.moka.api.keys.KeysBindOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.json.KeysBindOutputJson;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "bind",
	header = "Bind a key pair to a reference, thus creating an account's key pair file.",
	showDefaultValues = true)
public class Bind extends AbstractMokaRpcCommand {

	@Parameters(index = "0", description = "the path of the key pair")
    private Path key;

	@Option(names = "--output-dir", paramLabel = "<path>", description = "the path of the directory where the key pair of the bound account will be written", defaultValue = "")
    private Path outputDir;

	@Option(names = "--reference", paramLabel = "<storage reference>", description = "the account; if missing, it means that the account was created anonymously and its reference will be recovered from the accounts ledger of the Hotmoka node", converter = StorageReferenceOptionConverter.class)
    private StorageReference reference;

	@Option(names = "--password", description = "the password of the key pair", interactive = true, defaultValue = "")
    private char[] password;

	@Option(names = "--signature", description = "the signature algorithm for the key pair (ed25519, sha256dsa, qtesla1, qtesla3)", converter = SignatureOptionConverter.class, defaultValue = "ed25519")
	private SignatureAlgorithm signature;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		StorageReference reference = this.reference != null ? verifyPublicKey(remote) : getFromAccountsLedger(remote);
		Path file = bindKeysToAccount(key, reference, outputDir);
		report(json(), new Output(reference, file), KeysBindOutputs.Encoder::new);
	}

	/**
	 * Checks if {@link #reference} exists in the remote node and its public key coincides with that in the {@link key} file.
	 * 
	 * @param remote the remote node
	 * @return {@link #reference} itself
	 * @throws TimeoutException if the operation times out
	 * @throws InterruptedException of the operation gets interrupted
	 * @throws NodeException if the node is misbehaving
	 * @throws CommandException if {@link #reference} does not exist in the remote node or its public key does not
	 *                          coincide with that in the {@link #key} file
	 */
	private StorageReference verifyPublicKey(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		var manifest = remote.getManifest();
		var takamakaCode = remote.getTakamakaCode();
		String publicKeyBase64FromKeyFile = publicKeyBase64FromKeyFile();
		String publicKeyFromAccount;

		try {
			// we look for the public key in the account
			publicKeyFromAccount = remote.runInstanceMethodCallTransaction
					(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, reference))
					.orElseThrow(() -> new CommandException(MethodSignatures.PUBLIC_KEY + " should not return void"))
					.asReturnedString(MethodSignatures.PUBLIC_KEY, CommandException::new);
		}
		catch (CodeExecutionException | TransactionException | TransactionRejectedException e) {
			throw new CommandException("Could not access the public key of object " + reference, e);
		}

		if (!publicKeyBase64FromKeyFile.equals(publicKeyFromAccount))
			throw new CommandException("The public key in file " + key + " does not match the public key of object " + reference + " in the node: are you sure that reference, key pair file and password are correct?");

		return reference;
	}

	private StorageReference getFromAccountsLedger(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		var manifest = remote.getManifest();
		var takamakaCode = remote.getTakamakaCode();
		String publicKeyBase64FromKeyFile = publicKeyBase64FromKeyFile();
		StorageValue result;

		try {
			// we look in the accounts ledger
			var ledger = remote.runInstanceMethodCallTransaction
					(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_ACCOUNTS_LEDGER + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_ACCOUNTS_LEDGER, CommandException::new);

			result = remote.runInstanceMethodCallTransaction
					(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, MethodSignatures.GET_FROM_ACCOUNTS_LEDGER, ledger, StorageValues.stringOf(publicKeyBase64FromKeyFile)))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_FROM_ACCOUNTS_LEDGER + " should not return void"));
		}
		catch (CodeExecutionException | TransactionException | TransactionRejectedException e) {
			throw new CommandException("Could not access the accounts ledger", e);
		}

		if (result instanceof StorageReference sr)
			return sr;
		else if (result instanceof NullValue)
			throw new CommandException("Cannot bind: nobody has paid anonymously to the key " + key + " up to now.");
		else
			throw new CommandException("An unexpected value of type " + result.getClass().getSimpleName() + " has been found in the accounts ledger");
	}

	private String publicKeyBase64FromKeyFile() throws CommandException {
		String passwordAsString;

		try {
			passwordAsString = new String(password);
			return Base64.toBase64String(signature.encodingOf(Entropies.load(key).keys(passwordAsString, signature).getPublic()));
		}
		catch (IOException e) {
			throw new CommandException("Cannot read the file \"" + key + "\"", e);
		}
		catch (InvalidKeyException e) {
			throw new CommandException("The file \"" + key + "\" contains an invalid key for the signature algorithm " + signature);
		}
		finally {
			passwordAsString = null;
			Arrays.fill(password, ' ');
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements KeysBindOutput {

		/**
		 * The path of the created key pair file for the account that has been bound.
		 */
		private final Path file;

		/**
		 * The reference of the bound account.
		 */
		private final StorageReference account;

		/**
		 * Builds the output of the command.
		 */
		private Output(StorageReference account, Path file) {
			this.account = account;
			this.file = file;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(KeysBindOutputJson json) throws InconsistentJsonException {
			try {
				this.file = Paths.get(Objects.requireNonNull(json.getFile(), "file cannot be null", InconsistentJsonException::new));
			}
			catch (InvalidPathException e) {
				throw new InconsistentJsonException(e);
			}

			this.account = Objects.requireNonNull(json.getAccount(), "account cannot be null", InconsistentJsonException::new).unmap()
				.asReference(value -> new InconsistentJsonException("The reference of the bound account must be a storage reference, not a " + value.getClass().getName()));
		}

		@Override
		public Path getFile() {
			return file;
		}

		@Override
		public StorageReference getAccount() {
			return account;
		}

		@Override
		public String toString() {
			return "The key pair of " + account + " has been saved as " + asPath(file) + ".\n";
		}
	}
}