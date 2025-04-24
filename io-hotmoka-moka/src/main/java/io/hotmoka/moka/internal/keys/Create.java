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
import java.util.Objects;
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
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
import io.hotmoka.moka.keys.KeysCreateOutput;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Create a new key pair.")
public class Create extends AbstractCommand {

	@Option(names = "--dir", description = "the path of the directory where the key pair file of the new key pair must be written", defaultValue = "")
    private Path dir;

	@Option(names = "--name", description = "the name of the file where the new key pair must be written; if missing, the first characters of the Base58-encoded public key will be used, followed by \".pem\"")
    private String name;

	@Option(names = "--password", description = "the password that will be needed later to use the key pair", interactive = true, defaultValue = "")
    private char[] password;

	@Option(names = "--signature", description = "the signature algorithm for the key pair (ed25519, sha256dsa, qtesla1, qtesla3)",
			converter = SignatureOptionConverter.class, defaultValue = "ed25519")
	private SignatureAlgorithm signature;

	@Option(names = "--show-private", description = "show the private key")
	private boolean showPrivate;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	@Override
	protected void execute() throws CommandException {
		String passwordAsString;

		try {
			var entropy = Entropies.random();
			passwordAsString = new String(password);
			KeyPair keys = entropy.keys(passwordAsString, signature);

			String name = this.name;
			if (name == null) {
				try {
					name = Base58.toBase58String(signature.encodingOf(keys.getPublic()));
					if (name.length() > 100)
						name = name.substring(0, 100);
				}
				catch (InvalidKeyException e) {
					// this should not happen since we created the keys with the signature algorithm
					throw new RuntimeException(e);
				}

				name = name + ".pem";
			}

			Path file = dir.resolve(name);

			try {
				entropy.dump(file);
			}
			catch (IOException e) {
				throw new CommandException("Cannot write the key pair into " + file + "!", e);
			}

			try {
				System.out.println(new Output(signature, keys, showPrivate).toString(file, json));
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException("The sha256 hashing algorithm is not available in this machine!");
			}
		}
		finally {
			passwordAsString = null;
			Arrays.fill(password, ' ');
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements KeysCreateOutput {
		private final String signature;
		private final String publicKeyBase58;
		private final String publicKeyBase64;
		private final String tendermintAddress;
		private final String privateKeyBase58;
		private final String privateKeyBase64;
		private final String concatenatedBase64;

		/**
		 * Yields the output of this command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 */
		public static Output of(String json) {
			return new Gson().fromJson(json, Output.class);
		}

		public Output(SignatureAlgorithm signature, KeyPair keys, boolean alsoPrivate) throws NoSuchAlgorithmException {
			this.signature = signature.getName();

			try {
				byte[] publicKeyBytes = signature.encodingOf(keys.getPublic());
				byte[] privateKey = signature.encodingOf(keys.getPrivate());
				var concatenated = new byte[privateKey.length + publicKeyBytes.length];
				System.arraycopy(privateKey, 0, concatenated, 0, privateKey.length);
				System.arraycopy(publicKeyBytes, 0, concatenated, privateKey.length, publicKeyBytes.length);
				byte[] sha256HashedKey = HashingAlgorithms.sha256().getHasher(Function.identity()).hash(publicKeyBytes);
				this.publicKeyBase58 = Base58.toBase58String(publicKeyBytes);
				this.publicKeyBase64 = Base64.toBase64String(publicKeyBytes);

				if (alsoPrivate) {
					this.privateKeyBase58 = Base58.toBase58String(privateKey);
					this.privateKeyBase64 = Base64.toBase64String(privateKey);
					this.concatenatedBase64 = Base64.toBase64String(concatenated);
				}
				else {
					this.privateKeyBase58 = null;
					this.privateKeyBase64 = null;
					this.concatenatedBase64 = null;
				}

				this.tendermintAddress = Hex.toHexString(sha256HashedKey, 0, 20).toUpperCase();
			}
			catch (InvalidKeyException | HexConversionException e) {
				// this should not happen since we created the keys from the signature algorithm
				throw new RuntimeException("The new key pair is invalid!", e);
			}
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof KeysCreateOutput kco
					&& kco.getSignature().equals(signature)
					&& kco.getPublicKeyBase58().equals(publicKeyBase58)
					&& kco.getPublicKeyBase64().equals(publicKeyBase64)
					&& Objects.equals(kco.getPrivateKeyBase58(), privateKeyBase58)
					&& Objects.equals(kco.getPrivateKeyBase64(), privateKeyBase64)
					&& Objects.equals(kco.getConcatenatedBase64(), concatenatedBase64);
		}

		@Override
		public int hashCode() {
			return signature.hashCode() ^ publicKeyBase58.hashCode() ^ privateKeyBase58.hashCode();
		}

		@Override
		public String getSignature() {
			return signature;
		}

		@Override
		public String getPublicKeyBase58() {
			return publicKeyBase58;
		}

		@Override
		public String getPublicKeyBase64() {
			return publicKeyBase64;
		}

		@Override
		public String getTendermintAddress() {
			return tendermintAddress;
		}

		@Override
		public String getPrivateKeyBase58() {
			return privateKeyBase58;
		}

		@Override
		public String getPrivateKeyBase64() {
			return privateKeyBase64;
		}

		@Override
		public String getConcatenatedBase64() {
			return concatenatedBase64;
		}

		@Override
		public String toString(Path file, boolean json) {
			if (json)
				return new Gson().toJson(this);
			else {
				String result = "The new key pair has been written into \"" + file + "\":";

				if (publicKeyBase58.length() > MAX_PRINTED_KEY)
					result = "* public key: " + publicKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)";
				else
					result = "* public key: " + publicKeyBase58 + " (" + signature + ", base58)";

				if (publicKeyBase64.length() > MAX_PRINTED_KEY)
					result += "\n* public key: " + publicKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)";
				else
					result += "\n* public key: " + publicKeyBase64 + " (" + signature + ", base64)";

				result += "\n* Tendermint-like address: " + tendermintAddress;

				if (privateKeyBase58 != null) {
					if (privateKeyBase58.length() > MAX_PRINTED_KEY)
						result += "\n* private key: " + privateKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)";
					else
						result += "\n* private key: " + privateKeyBase58 + " (" + signature + ", base58)";
				}

				if (privateKeyBase64 != null) {
					if (privateKeyBase64.length() > MAX_PRINTED_KEY)
						result += "\n* private key: " + privateKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)";
					else
						result += "\n* private key: " + privateKeyBase64 + " (" + signature + ", base64)";
				}

				if (concatenatedBase64 != null) {
					if (concatenatedBase64.length() > MAX_PRINTED_KEY * 2)
						result += "\n* concatenated private+public key: " + concatenatedBase64.substring(0, MAX_PRINTED_KEY * 2) + "..." + " (base64)";
					else
						result += "\n* concatenated private+public key: " + concatenatedBase64 + " (base64)";
				}

				return result;
			}
		}
	}
}