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
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.hotmoka.cli.AbstractCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.BIP39Mnemonic;
import io.hotmoka.moka.KeysExportOutputs;
import io.hotmoka.moka.api.keys.KeysExportOutput;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
import io.hotmoka.moka.internal.json.KeysExportOutputJson;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import jakarta.websocket.EncodeException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "export",
	description = "Export a key pair file of an account as BIP39 words.",
	showDefaultValues = true)
public class Export extends AbstractCommand {

	@Parameters(index = "0", description = "the reference of the account to export in BIP39 words", converter = StorageReferenceOfAccountOptionConverter.class)
    private StorageReference reference;

	@Option(names = "--dir", description = "the path of the directory where the key pair of the account can be found", defaultValue = "")
    private Path dir;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	@Override
	protected void execute() throws CommandException {
        Account account;

        try {
        	account = Accounts.of(reference, dir);
        }
        catch (IOException e) {
        	throw new CommandException("Cannot read the key pair of the account: it was expected to be in file \"" + reference + ".pem\"", e);
        }

        new Output(account.bip39Words()).println(System.out, reference, json);
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements KeysExportOutput {
		private final String[] bip39Words;

		private Output(BIP39Mnemonic bip39Words) {
			this.bip39Words = bip39Words.stream().toArray(String[]::new);
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(KeysExportOutputJson json) throws InconsistentJsonException {
			this.bip39Words = json.getBip39Words().toArray(String[]::new);
			if (bip39Words.length != 36)
				throw new InconsistentJsonException("Hotmoka accounts are represented by 36 BIP39 words, but " + bip39Words.length + " words have been provided instead");
		}

		@Override
		public Stream<String> getBip39Words() {
			return Stream.of(bip39Words);
		}

		@Override
		public void println(PrintStream out, StorageReference reference, boolean json) {
			if (json) {
				try {
					out.println(new KeysExportOutputs.Encoder().encode(this));
				}
				catch (EncodeException e) {
					// this should not happen, since the constructor of the JSON representation never throws exceptions
					throw new RuntimeException("Cannot encode the output of the command in JSON format", e);
				}
			}
	        else {
	        	out.println("The following BIP39 words represent the key pair of account " + reference + ":");
	        	for (int pos = 0; pos < bip39Words.length; pos++)
	        		out.println(String.format("%2d: %s", pos + 1, bip39Words[pos]));
	        }
		}
	}
}