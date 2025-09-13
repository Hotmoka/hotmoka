/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.internal.nodes.tendermint.validators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.NodesTendermintValidatorsKeyOutputs;
import io.hotmoka.moka.api.nodes.tendermint.validators.NodesTendermintValidatorsKeyOutput;
import io.hotmoka.moka.internal.AbstractMokaCommand;
import io.hotmoka.moka.internal.json.NodesTendermintValidatorsKeyOutputJson;
import io.hotmoka.moka.internal.json.TendermintPrivValidatorJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "key", header = "Generate a Tendermint validator key file from a Hotmoka Ed25519 key pair.", showDefaultValues = true)
public class Key extends AbstractMokaCommand {

	@Parameters(index = "0", description = "the path of the file holding the key pair")
    private Path keys;

	@Option(names = "--output-dir", paramLabel = "<path>", description = "the directory where the Tendermint validator key file will be written", defaultValue = "")
    private Path outputDir;

	@Option(names = "--name", description = "the name to give to the generated Tendermint validator key file", defaultValue = "priv_validator_key.json")
    private String name;

	@Option(names = "--password", description = "the password of the key pair", interactive = true, defaultValue = "")
    private char[] password;

	@Override
	protected void execute() throws CommandException {
		String passwordAsString = new String(password);

		try {
			// Tendermint only works with Ed25519 key pairs
			SignatureAlgorithm signature;

			try {
				signature = SignatureAlgorithms.ed25519();
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException("The Ed25519 signature algorithm is not available");
			}

			KeyPair keys;

			try {
				keys = Entropies.load(this.keys).keys(passwordAsString, signature);
			}
			catch (IOException e) {
				throw new CommandException("Cannot access file \"" + this.keys + "\"", e);
			}

			byte[] publicKeyBytes, privateKeyBytes;

			try {
				publicKeyBytes = signature.encodingOf(keys.getPublic());
				privateKeyBytes = signature.encodingOf(keys.getPrivate());
			}
			catch (InvalidKeyException e) {
				throw new CommandException("The key pair is invalid: are you sure that it is an Ed25519 key pair?");
			}

			String publicKeyBase64 = Base64.toBase64String(publicKeyBytes);

			byte[] sha256HashedKey;
			try {
				sha256HashedKey = HashingAlgorithms.sha256().getHasher(Function.identity()).hash(publicKeyBytes);
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException("The sha256 hashing algorithm is not available");
			}

			String tendermintAddress;
			try {
				tendermintAddress = Hex.toHexString(sha256HashedKey, 0, 20).toUpperCase();
			}
			catch (HexConversionException e) {
				// this should not happen since the output of the sha256 algorithm can be converted into a hex string
				throw new RuntimeException("The output of the sha256 algorithm is invalid!", e);
			}

			var concatenated = new byte[privateKeyBytes.length + publicKeyBytes.length];
			System.arraycopy(privateKeyBytes, 0, concatenated, 0, privateKeyBytes.length);
			System.arraycopy(publicKeyBytes, 0, concatenated, privateKeyBytes.length, publicKeyBytes.length);
			String concatenatedBase64 = Base64.toBase64String(concatenated);

			Path path = outputDir.resolve(name);
			Gson gson = new GsonBuilder().disableHtmlEscaping().create();

			try {
				Files.writeString(path, gson.toJson(new TendermintPrivValidatorJson(publicKeyBase64, tendermintAddress, concatenatedBase64)));
			}
			catch (IOException e) {
				throw new CommandException("Cannot write into \"" + path + "\"", e);
			}

			report(new Output(path), NodesTendermintValidatorsKeyOutputs.Encoder::new);
		}
		finally {
			passwordAsString = null;
			Arrays.fill(password, ' ');
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements NodesTendermintValidatorsKeyOutput {

		/**
		 * The path of the Tendermint validator key file that has been generated.
		 */
		private final Path file;

		/**
		 * Builds the output of the command.
		 */
		private Output(Path file) {
			this.file = file;
		}
	
		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesTendermintValidatorsKeyOutputJson json) throws InconsistentJsonException {
			try {
				this.file = Paths.get(Objects.requireNonNull(json.getFile(), "file cannot be null", InconsistentJsonException::new));
			}
			catch (InvalidPathException e) {
				throw new InconsistentJsonException(e);
			}
		}

		@Override
		public Path getFile() {
			return file;
		}

		@Override
		public String toString() {
			return "The Tendermint validator key has been saved in file " + asPath(file) + ".\n";
		}
	}
}