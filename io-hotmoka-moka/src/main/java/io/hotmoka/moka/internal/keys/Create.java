/*
Copyright 2023 Fausto Spoto

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
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;

import com.google.gson.Gson;

import io.hotmoka.cli.AbstractCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.moka.internal.KeysInfo;
import io.hotmoka.moka.internal.SignatureOptionConverter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "Create a new key pair")
public class Create extends AbstractCommand {

	@Parameters(index = "0", description = "the file where the key pair will be stored")
	private Path path;

	@Option(names = "--password", description = "the password that will be needed later to use the key pair", interactive = true, defaultValue = "")
    private char[] password;

	@Option(names = "--signature", description = "the signature algorithm for the key pair (ed25519, sha256dsa, qtesla1, qtesla3)",
			converter = SignatureOptionConverter.class, defaultValue = "ed25519")
	private SignatureAlgorithm signature;

	@Option(names = { "--private-key" }, description = "show the private key of the account")
	private boolean privateKey;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	@Override
	protected void execute() throws CommandException {
		String passwordAsString;

		try {
			var entropy = Entropies.random();
			passwordAsString = new String(password);
			KeyPair keys = entropy.keys(passwordAsString, signature);
			var keysInfo = new KeysInfo(signature, keys, privateKey);

			try {
				entropy.dump(path);
			}
			catch (IOException e) {
				throw new CommandException("The key pair could not be dumped into " + path + "!", e);
			}

			if (json)
				System.out.println(new Gson().toJsonTree(keysInfo));
			else {
				System.out.println("The new key pair has been saved into \"" + path + "\":");
				System.out.println(keysInfo);
			}
		}
		finally {
			passwordAsString = null;
			Arrays.fill(password, ' ');
		}
	}
}