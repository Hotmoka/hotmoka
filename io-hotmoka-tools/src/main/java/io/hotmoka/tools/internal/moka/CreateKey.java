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

import java.security.KeyPair;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Entropy;
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create-key",
	description = "Creates a new key",
	showDefaultValues = true)
public class CreateKey extends AbstractCommand {

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = { "--password-of-new-key" }, description = "the password that will be used later to control a new account; if not specified, it will be asked interactively")
    private String passwordOfNewKey;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode")
	private boolean nonInteractive;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {

		private Run() throws Exception {
			passwordOfNewKey = ensurePassword(passwordOfNewKey, "the new key", nonInteractive, false);
			var signatureAlgorithmOfNewAccount = SignatureAlgorithmForTransactionRequests.ed25519();
			Entropy entropy = new Entropy();
			KeyPair keys = entropy.keys(passwordOfNewKey, signatureAlgorithmOfNewAccount);
			var publicKeyBase58 = Base58.encode(signatureAlgorithmOfNewAccount.encodingOf(keys.getPublic()));
			System.out.println("A new key " + publicKeyBase58 + " has been created.");
			String fileName = entropy.dump(publicKeyBase58);
			System.out.println("Its entropy has been saved into the file \"" + fileName + "\".");
		}
	}
}