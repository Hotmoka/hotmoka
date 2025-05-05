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

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.api.GasCost;
import io.hotmoka.moka.AccountsCreateOutputs;
import io.hotmoka.moka.api.accounts.AccountsCreateOutput;
import io.hotmoka.moka.internal.AbstractAccountCreation;
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
import io.hotmoka.moka.internal.json.AccountsCreateOutputJson;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", header = "Create a new account object.", showDefaultValues = true)
public class Create extends AbstractAccountCreation {

	@Option(names = "--signature", description = "the signature algorithm of the new account (ed25519, sha256dsa, qtesla1, qtesla3); if missing, the default request signature of the node will be used", converter = SignatureOptionConverter.class)
	private SignatureAlgorithm signature;

	@Override
	protected SignatureAlgorithm getSignatureAlgorithmOfNewAccount(RemoteNode remote) throws NodeException, TimeoutException, InterruptedException {
		return signature != null ? signature : remote.getConfig().getSignatureForRequests();
	}

	@Override
	protected NonVoidMethodSignature getFaucetMethod(SignatureAlgorithm signatureOfNewAccount, ClassType eoaType) {
		return MethodSignatures.ofNonVoid(StorageTypes.GAMETE, "faucet" + signatureOfNewAccount.getName().toUpperCase(), eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING);
	}

	@Override
	protected ClassType getEOAType(SignatureAlgorithm signatureOfNewAccount) throws CommandException {
		switch (signatureOfNewAccount.getName()) {
		case "ed25519":
		case "sha256dsa":
		case "qtesla1":
		case "qtesla3":
			return StorageTypes.classNamed(StorageTypes.EOA + signatureOfNewAccount.getName().toUpperCase());
		default:
			throw new CommandException("Cannot create accounts with signature algorithm " + signatureOfNewAccount);
		}
	}

	@Override
	protected void reportOutput(TransactionReference transaction, StorageReference referenceOfNewAccount, Optional<Path> file, GasCost gasCosts) throws CommandException {
		report(json(), new Output(transaction, referenceOfNewAccount, file, gasCosts), AccountsCreateOutputs.Encoder::new);
	}

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractAccountCreationOutput implements AccountsCreateOutput {

		/**
		 * Builds the output of the command.
		 */
		private Output(TransactionReference transaction, StorageReference account, Optional<Path> file, GasCost gasCounter) {
			super(transaction, account, file, gasCounter);
		}

		public Output(AccountsCreateOutputJson json) throws InconsistentJsonException {
			super(json);
		}
	}
}