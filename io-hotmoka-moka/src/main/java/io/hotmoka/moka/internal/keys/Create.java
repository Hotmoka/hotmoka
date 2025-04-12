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
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Function;

import com.google.gson.Gson;

import io.hotmoka.cli.AbstractCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
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

	/**
	 * The maximal length for the printed keys. After this length, the printout of the key gets truncated.
	 */
	public final static int MAX_PRINTED_KEY = 200;

	@Override
	protected void execute() throws CommandException {
		String passwordAsString;

		try {
			var entropy = Entropies.random();
			passwordAsString = new String(password);
			KeyPair keys = entropy.keys(passwordAsString, signature);
			byte[] publicKeyBytes = signature.encodingOf(keys.getPublic());
			byte[] privateKey = signature.encodingOf(keys.getPrivate());
			var concatenated = new byte[privateKey.length + publicKeyBytes.length];
			System.arraycopy(privateKey, 0, concatenated, 0, privateKey.length);
			System.arraycopy(publicKeyBytes, 0, concatenated, privateKey.length, publicKeyBytes.length);
			byte[] sha256HashedKey = HashingAlgorithms.sha256().getHasher(Function.identity()).hash(publicKeyBytes);

			var answer = new Answer(signature);
			answer.publicKeyBase58 = Base58.toBase58String(publicKeyBytes);
			answer.publicKeyBase64 = Base64.toBase64String(publicKeyBytes);

			if (this.privateKey) {
				answer.privateKeyBase58 = Base58.toBase58String(privateKey);
				answer.privateKeyBase64 = Base64.toBase64String(privateKey);
				answer.concatenatedBase64 = Base64.toBase64String(concatenated);
			}

			answer.tendermintAddress = Hex.toHexString(sha256HashedKey, 0, 20).toUpperCase();
			answer.fileName = path.toString();
			entropy.dump(path);

			if (json)
				System.out.println(new Gson().toJsonTree(answer));
			else
				System.out.println(answer);
		}
		catch (IOException e) {
			throw new CommandException("The key pair could not be dumped into a file!", e);
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("The sha256 hashing algorithm is not available on this machine!", e);
		}
		catch (InvalidKeyException | HexConversionException e) {
			// this should not happen since we created the keys from the signature algorithm
			throw new RuntimeException("The new key pair is invalid!", e);
		}
		finally {
			passwordAsString = null;
			Arrays.fill(password, ' ');
		}
	}

	private static class Answer {
		private final transient SignatureAlgorithm signature;
		private String publicKeyBase58;
		private String publicKeyBase64;
		private String privateKeyBase58;
		private String privateKeyBase64;
		private String concatenatedBase64;
		private String tendermintAddress;
		private String fileName;

		private Answer(SignatureAlgorithm signature) {
			this.signature = signature;
		}

		public String toString() {
			String result = "The new key pair has been saved as \"" + fileName + "\":\n";

			if (publicKeyBase58.length() > MAX_PRINTED_KEY)
				result += "* public key: " + publicKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)\n";
			else
				result += "* public key: " + publicKeyBase58 + " (" + signature + ", base58)\n";

			if (publicKeyBase64.length() > MAX_PRINTED_KEY)
				result += "* public key: " + publicKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)\n";
			else
				result += "* public key: " + publicKeyBase64 + " (" + signature + ", base64)\n";

			if (privateKeyBase58 != null) {
				if (privateKeyBase58.length() > MAX_PRINTED_KEY)
					result += "* private key: " + privateKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)\n";
				else
					result += "* private key: " + privateKeyBase58 + " (" + signature + ", base58)\n";
			}

			if (privateKeyBase64 != null) {
				if (privateKeyBase64.length() > MAX_PRINTED_KEY)
					result += "* private key: " + privateKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)\n";
				else
					result += "* private key: " + privateKeyBase64 + " (" + signature + ", base64)\n";
			}

			if (concatenatedBase64 != null) {
				if (concatenatedBase64.length() > MAX_PRINTED_KEY * 2)
					result += "* concatenated private+public key: " + concatenatedBase64.substring(0, MAX_PRINTED_KEY * 2) + "..." + " (base64)\n";
				else
					result += "* concatenated private+public key: " + concatenatedBase64 + " (base64)\n";
			}

			result += "* Tendermint-like address: " + tendermintAddress + "\n";

			return result;
		}
	}
}