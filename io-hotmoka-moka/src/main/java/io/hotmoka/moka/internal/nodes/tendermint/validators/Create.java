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

package io.hotmoka.moka.internal.nodes.tendermint.validators;

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.moka.NodesTendermintValidatorsCreateOutputs;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.api.nodes.tendermint.validators.NodesTendermintValidatorsCreateOutput;
import io.hotmoka.moka.internal.AbstractAccountCreation;
import io.hotmoka.moka.internal.json.NodesTendermintValidatorsCreateOutputJson;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;

@Command(name = "create", header = "Create a new validator account object.", showDefaultValues = true)
public class Create extends AbstractAccountCreation {

	@Override
	protected SignatureAlgorithm getSignatureAlgorithmOfNewAccount(RemoteNode remote) throws CommandException {
		// Tendermint can only use the ed25519 signature
		try {
			return SignatureAlgorithms.ed25519();
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("Tendermint uses the ed25519 signature algorithm, but the latter is not available");
		}
	}

	@Override
	protected NonVoidMethodSignature getFaucetMethod(SignatureAlgorithm signatureOfNewAccount, ClassType eoaType) {
		return MethodSignatures.ofNonVoid(StorageTypes.GAMETE, "faucetTendermintED25519Validator", StorageTypes.TENDERMINT_ED25519_VALIDATOR, StorageTypes.BIG_INTEGER, StorageTypes.STRING);
	}

	@Override
	protected ClassType getEOAType(SignatureAlgorithm signatureOfNewAccount) {
		return StorageTypes.TENDERMINT_ED25519_VALIDATOR;
	}

	@Override
	protected void reportOutput(StorageReference referenceOfNewAccount, Optional<Path> file, GasCost gasCost, BigInteger gasPrice) throws CommandException {
		report(json(), new Output(referenceOfNewAccount, file, gasCost, gasPrice), NodesTendermintValidatorsCreateOutputs.Encoder::new);
	}

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractAccountCreationOutput implements NodesTendermintValidatorsCreateOutput {

		private Output(StorageReference account, Optional<Path> file, GasCost gasCost, BigInteger gasPrice) {
			super(account, file, gasCost, gasPrice);
		}

		public Output(NodesTendermintValidatorsCreateOutputJson json) throws InconsistentJsonException {
			super(json);
		}
	}
}