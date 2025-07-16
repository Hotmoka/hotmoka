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

package io.hotmoka.moka.internal.nodes;

import static io.hotmoka.node.StorageTypes.BIG_INTEGER;
import static io.hotmoka.node.StorageTypes.GAMETE;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.MisbehavingNodeException;
import io.hotmoka.moka.NodesFaucetOutputs;
import io.hotmoka.moka.api.nodes.NodesFaucetOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.converters.NonNegativeBigIntegerOptionConverter;
import io.hotmoka.moka.internal.json.NodesFaucetOutputJson;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnexpectedCodeException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "faucet",
	header = "Set the threshold for the faucet of the gamete of a node.",
	showDefaultValues = true)
public class Faucet extends AbstractMokaRpcCommand {

	@Parameters(description = "the maximal amount of coins sent at each call to the faucet of the node", converter = NonNegativeBigIntegerOptionConverter.class)
    private BigInteger max;

	@Option(names = "--dir", paramLabel = "<path>", description = "the path of the directory where the key pair of the gamete can be found", defaultValue = "")
    private Path dir;

	@Option(names = "--password", description = "the password of the gamete account", interactive = true, defaultValue = "")
    private char[] password;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, ClosedNodeException, UninitializedNodeException, MisbehavingNodeException, UnexpectedCodeException {
		var manifest = remote.getManifest();
		var takamakaCode = remote.getTakamakaCode();

		StorageReference gamete;

		try {
			gamete = remote.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
				.orElseThrow(() -> new CommandException(MethodSignatures.GET_GAMETE + " should not return void"))
				.asReturnedReference(MethodSignatures.GET_GAMETE, CommandException::new);
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new CommandException("Cannot access the gamete of the node!", e);
		}

		String chainId = remote.getConfig().getChainId();
		SignatureAlgorithm signature;

		try {
			signature = SignatureHelpers.of(remote).signatureAlgorithmFor(gamete);
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("The gamete of the node uses a non-available signature algorithm", e);
		}
		catch (UnknownReferenceException e) {
			throw new CommandException("The gamete object cannot be found in the node");
		}
		catch (UnsupportedVerificationVersionException e) {
			throw new CommandException("The node uses a verification version that is not available");
		}

		String passwordAsString;
		KeyPair keys;

		try {
			passwordAsString = new String(password);
			keys = Accounts.of(gamete, dir).keys(passwordAsString, signature);
		}
		catch (IOException e) {
			throw new CommandException("Cannot read the key pair of the gamete: it was expected to be in file " + gamete + ".pem", e);
		}
		finally {
			passwordAsString = null;
			Arrays.fill(password, ' ');
		}

		try {
			remote.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
				(signature.getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
				gamete, NonceHelpers.of(remote).getNonceOf(gamete),
				chainId, _100_000, GasHelpers.of(remote).getGasPrice(), takamakaCode,
				MethodSignatures.ofVoid(GAMETE, "setMaxFaucet", BIG_INTEGER), gamete,
				StorageValues.bigIntegerOf(max)));
		}
		catch (InvalidKeyException e) {
			throw new CommandException("The key file for the gamete contains an invalid key");
		}
		catch (SignatureException e) {
			throw new CommandException("Failed to sign with the key pair for the gamete of the node");
		}
		catch (TransactionRejectedException e) {
			throw new CommandException("Cannot set the threshold of the faucet of the node: are you sure that the password of the gamete is correct?", e);
		}
		catch (TransactionException | CodeExecutionException e) {
			throw new CommandException("Cannot set the threshold of the faucet of the node", e);
		}
		catch (UnknownReferenceException e) {
			throw new CommandException("The gamete object cannot be found in the node");
		}

		report(json(), new Output(), NodesFaucetOutputs.Encoder::new);
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements NodesFaucetOutput {

		private Output() {}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesFaucetOutputJson json) throws InconsistentJsonException {}

		@Override
		public String toString() {
			return "The threshold of the faucet has been set.\n";
		}
	}
}