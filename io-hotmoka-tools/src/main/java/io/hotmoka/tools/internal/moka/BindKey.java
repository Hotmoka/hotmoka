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

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.Account;
import io.hotmoka.crypto.Entropy;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "bind-key",
	description = "Binds a key to a reference, so that it becomes an account",
	showDefaultValues = true)
public class BindKey extends AbstractCommand {

	@Parameters(index = "0", description = "the key that gets bound to the reference")
    private String key;

	@Parameters(index = "1", description = "the reference to bind to the key")
    private String reference;

	@Override
	protected void execute() throws Exception {
		checkPublicKey(key);
		checkStorageReference(reference);
		Account account = new Account(new Entropy(key), new StorageReference(reference));
		String newFileName = account.dump();
		System.out.println("A new account " + reference + " has been created.");
        System.out.println("The entropy of the account has been saved into the file " + newFileName);
	}
}