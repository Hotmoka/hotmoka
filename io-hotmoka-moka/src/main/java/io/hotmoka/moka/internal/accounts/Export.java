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
import java.nio.file.Path;
import java.util.stream.Stream;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.BIP39Mnemonic;
import io.hotmoka.moka.AccountsExportOutputs;
import io.hotmoka.moka.api.accounts.AccountsExportOutput;
import io.hotmoka.moka.internal.AbstractMokaCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceWithZeroProgressiveOptionConverter;
import io.hotmoka.moka.internal.json.AccountsExportOutputJson;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "export",
	header = "Export a key pair file of an account as BIP39 words.",
	showDefaultValues = true)
public class Export extends AbstractMokaCommand {

	@Parameters(index = "0", description = "the storage reference of the account to export in BIP39 words", converter = StorageReferenceWithZeroProgressiveOptionConverter.class)
    private StorageReference reference;

	@Option(names = "--dir", paramLabel = "<path>", description = "the path of the directory where the key pair of the account can be found", defaultValue = "")
    private Path dir;

	@Override
	protected void execute() throws CommandException {
        Account account;

        try {
        	account = Accounts.of(reference, dir);
        }
        catch (IOException e) {
        	throw new CommandException("Cannot read the key pair of the account: it was expected to be in file \"" + dir.resolve(reference.toString()) + ".pem\"", e);
        }

        report(new Output(account.bip39Words()), AccountsExportOutputs.Encoder::new);
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements AccountsExportOutput {
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
		public Output(AccountsExportOutputJson json) throws InconsistentJsonException {
			this.bip39Words = json.getBip39Words().toArray(String[]::new);
			if (bip39Words.length != 36)
				throw new InconsistentJsonException("Hotmoka accounts are represented by 36 BIP39 words, but " + bip39Words.length + " words have been provided instead");
		}

		@Override
		public Stream<String> getBip39Words() {
			return Stream.of(bip39Words);
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			sb.append("The following BIP39 words represent the key pair of the account:\n");
        	for (int pos = 0; pos < bip39Words.length; pos++)
        		sb.append(String.format("%2d: %s\n", pos + 1, bip39Words[pos]));

        	return sb.toString();
		}
	}
}