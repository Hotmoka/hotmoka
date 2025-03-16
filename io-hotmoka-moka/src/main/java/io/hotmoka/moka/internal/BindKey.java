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

package io.hotmoka.moka.internal;

import java.net.URI;
import java.nio.file.Paths;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.remote.RemoteNodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "bind-key",
	description = "Bind a key to a reference, so that it becomes an account",
	showDefaultValues = true)
public class BindKey extends AbstractCommand {

	@Parameters(index = "0", description = "the key that gets bound to the reference")
    private String key;

	@Option(names = { "--reference" }, description = "the reference to bind to the key, or \"anonymous\" if the key has been charged anonymously", defaultValue = "anonymous")
    private String reference;

	@Option(names = { "--uri" }, description = "the URI of the node", defaultValue = "ws://localhost:8001")
    private URI uri;

	@Override
	protected void execute() throws Exception {
		checkPublicKey(key);

		StorageReference storageReference;
		if ("anonymous".equals(reference))
			storageReference = getReferenceFromAccountLedger();
		else {	
			checkStorageReference(reference);
			storageReference = StorageValues.reference(reference, CommandException::new);
		}

		var account = Accounts.of(Entropies.load(Paths.get(key + ".pem")), storageReference);
		System.out.println("A new account " + account + " has been created.");
		System.out.println("Its entropy has been saved into the file \"" + account.dump() + "\".");
	}

	private StorageReference getReferenceFromAccountLedger() throws Exception {
		try (var node = RemoteNodes.of(uri, 10_000)) {
			var manifest = node.getManifest();
			var takamakaCode = node.getTakamakaCode();

			// we must translate the key from Base58 to Base64
			String key = Base64.toBase64String(Base58.fromBase58String(this.key));

			// we look in the accounts ledger
			var ledger = node.runInstanceMethodCallTransaction
				(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest, StorageValues.NO_VALUES, IllegalArgumentException::new))
				.orElseThrow(() -> new CommandException(MethodSignatures.GET_ACCOUNTS_LEDGER + " should not return void"))
				.asReturnedReference(MethodSignatures.GET_ACCOUNTS_LEDGER, CommandException::new);
			StorageValue result = node.runInstanceMethodCallTransaction
				(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, MethodSignatures.GET_FROM_ACCOUNTS_LEDGER, ledger, new StorageValue[] { StorageValues.stringOf(key) }, IllegalArgumentException::new))
				.orElseThrow(() -> new CommandException(MethodSignatures.GET_FROM_ACCOUNTS_LEDGER + " should not return void"));

			if (result instanceof StorageReference sr)
				return sr;
			else
				throw new CommandException("Cannot bind: nobody has paid anonymously to the key " + this.key + " up to now.");
		}
	}
}