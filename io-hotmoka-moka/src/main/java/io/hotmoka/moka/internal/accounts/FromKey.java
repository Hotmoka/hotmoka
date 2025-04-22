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
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.SignatureOptionConverter;
import io.hotmoka.moka.internal.StorageReferenceOptionConverter;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.remote.api.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "from-key",
	description = "Create an account file from key and reference",
	showDefaultValues = true)
public class FromKey extends AbstractMokaRpcCommand {

	@Parameters(index = "0", description = "the file holding the key of the account")
    private Path key;

	@Option(names = "--reference", description = "the reference of the account; if missing, it means that the account was created anonymously and the reference must be recovered from the accounts ledger of the Hotmoka node", converter = StorageReferenceOptionConverter.class)
    private StorageReference reference;

	@Option(names = "--password", description = "the password of the key pair; this is used only if --reference is not provided", interactive = true, defaultValue = "")
    private char[] password;

	@Option(names = "--signature", description = "the signature algorithm for the key pair (ed25519, sha256dsa, qtesla1, qtesla3); this is used only if --reference is not provided",
			converter = SignatureOptionConverter.class, defaultValue = "ed25519")
	private SignatureAlgorithm signature;

	@Override
	protected void execute() throws CommandException {
		if (reference != null)
			createAccountFile(reference);
		else
			execute(this::body);
	}

	private void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		var manifest = remote.getManifest();
		var takamakaCode = remote.getTakamakaCode();

		String passwordAsString, publicKeyAsBase64String;
		try {
			passwordAsString = new String(password);
			publicKeyAsBase64String = Base64.toBase64String(signature.encodingOf(Entropies.load(key).keys(passwordAsString, signature).getPublic()));
		}
		catch (IOException e) {
			throw new CommandException("Cannot read the file \"" + key + "\"");
		}
		catch (InvalidKeyException e) {
			throw new CommandException("The file \"" + key + "\" contains a key invalid for the signature algorithm " + signature);
		}
		finally {
			passwordAsString = null;
			Arrays.fill(password, ' ');
		}

		var _100_000 = BigInteger.valueOf(100_000L);
		StorageValue result;

		try {
			// we look in the accounts ledger
			var ledger = remote.runInstanceMethodCallTransaction
					(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest))
					.orElseThrow(() -> new NodeException(MethodSignatures.GET_ACCOUNTS_LEDGER + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_ACCOUNTS_LEDGER, CommandException::new);

			result = remote.runInstanceMethodCallTransaction
					(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, MethodSignatures.GET_FROM_ACCOUNTS_LEDGER, ledger, StorageValues.stringOf(publicKeyAsBase64String)))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_FROM_ACCOUNTS_LEDGER + " should not return void"));
		}
		catch (CodeExecutionException | TransactionException | TransactionRejectedException e) {
			// the called methods should not throw exceptions
			throw new RuntimeException(e);
		}

		if (result instanceof StorageReference sr)
			createAccountFile(sr);
		else if (result instanceof NullValue)
			throw new CommandException("Cannot bind: nobody has paid anonymously to the key " + key + " up to now.");
		else
			throw new NodeException("An unexpected value of type " + result.getClass().getSimpleName() + " has been found in the accounts ledger");
	}

	private void createAccountFile(StorageReference reference) throws CommandException {
		if (reference.getProgressive().signum() != 0)
			throw new CommandException("Accounts must have references whose progressive is 0!");

		Account account;

		try {
			account = Accounts.of(Entropies.load(key), reference);
		}
		catch (IOException e) {
			throw new CommandException("Cannot read the file \"" + key + "\"");
		}

		try {
			System.out.println("The account information has been written into the file \"" + account.dump() + "\".");
		}
		catch (IOException e) {
			throw new CommandException("Cannot write the account information file");
		}
	}
}