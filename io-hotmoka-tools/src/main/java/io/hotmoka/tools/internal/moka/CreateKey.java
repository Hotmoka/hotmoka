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
import java.util.Base64;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Entropy;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create-key",
	description = "Creates a new key",
	showDefaultValues = true)
public class CreateKey extends AbstractCommand {

	@Option(names = { "--password-of-new-key" }, description = "the password that will be used later to control a new account; if not specified, it will be asked interactively")
    private String passwordOfNewKey;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true")
	private boolean interactive;

	@Option(names = { "--private-key" }, description = "show the private key of the account")
	private boolean privateKey;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {

		private Run() throws Exception {
			passwordOfNewKey = ensurePassword(passwordOfNewKey, "the new key", interactive, false);
			var signatureAlgorithmOfNewAccount = SignatureAlgorithmForTransactionRequests.ed25519();
			Entropy entropy = new Entropy();
			KeyPair keys = entropy.keys(passwordOfNewKey, signatureAlgorithmOfNewAccount);
			byte[] publicKeyBytes = signatureAlgorithmOfNewAccount.encodingOf(keys.getPublic());
			var publicKeyBase58 = Base58.encode(publicKeyBytes);
			System.out.println("A new key has been created.");
			System.out.println("Public key Base58: " + publicKeyBase58);
			System.out.println("Public key Base64: " + Base64.getEncoder().encodeToString(publicKeyBytes));

			if (privateKey) {
				byte[] privateKey = signatureAlgorithmOfNewAccount.encodingOf(keys.getPrivate());
				System.out.println("Private key Base58: " + Base58.encode(privateKey));
				System.out.println("Private key Base64: " + Base64.getEncoder().encodeToString(privateKey));
				byte[] concatenated = new byte[privateKey.length + publicKeyBytes.length];
				System.arraycopy(privateKey, 0, concatenated, 0, privateKey.length);
				System.arraycopy(publicKeyBytes, 0, concatenated, privateKey.length, publicKeyBytes.length);
				System.out.println("Concatenated private+public key Base64: " + Base64.getEncoder().encodeToString(concatenated));
			}

			byte[] hashedKey = HashingAlgorithm.sha256((byte[] bytes) -> bytes).hash(publicKeyBytes);
			String hex = Hex.toHexString(hashedKey, 0, 20).toUpperCase();
			System.out.println("Tendermint-like address: " + hex);

			String fileName = entropy.dump(publicKeyBase58);
			System.out.println("Its entropy has been saved into the file \"" + fileName + "\".");
		}
	}
}