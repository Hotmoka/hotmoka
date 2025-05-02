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

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base58ConversionException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.api.nodes.NodesInitOutput;
import io.hotmoka.moka.internal.AbstractMokaCommand;
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
import io.hotmoka.moka.internal.json.NodesInitOutputJson;
import io.hotmoka.node.api.nodes.ConsensusConfigBuilder;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Shared code for the moka commands that initialize a new Hotmoka node.
 */
public abstract class AbstractInit extends AbstractMokaCommand {

	@Parameters(description = "the path of the jar with the basic Takamaka classes that will be installed in the node")
	private Path takamakaCode;

	@Option(names = "--initial-supply", description = "the initial supply of coins of the node, which goes to the gamete")
	private BigInteger initialSupply;

	@Option(names = "--final-supply", description = "the final supply of coins of the node, after which inflation becomes 0")
	private BigInteger finalSupply;

	@Option(names = "--public-key-of-gamete", description = "the Base58-encoded ed25519 public key to use for the gamete account")
	private String publicKeyOfGamete;

	@Option(names = "--chain-id", description = "the chain identifier of the network")
	private String chainId;

	@Option(names = "--signature", description = "the default signature algorithm to use for signing the requests to the node (ed25519, sha256dsa, qtesla1, qtesla3)", converter = SignatureOptionConverter.class)
	private SignatureAlgorithm signature;
	
	@Option(names = "--open-unsigned-faucet", description = "open the unsigned faucet of the gamete") 
	private Boolean openUnsignedFaucet;

	@Option(names = "--initial-gas-price", description = "the initial price of a unit of gas") 
	private BigInteger initialGasPrice;

	@Option(names = "--oblivion", description = "how quick the gas consumed at previous rewards is forgotten (0 = never, 1000000 = immediately); use 0 to keep the gas price constant") 
	private Long oblivion;

	@Option(names = "--ignore-gas-price", description = "accept transactions regardless of their gas price") 
	private Boolean ignoreGasPrice;

	@Option(names = "--max-gas-per-view", description = "the maximal gas limit accepted for calls to @View methods") 
	private BigInteger maxGasPerView;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Option(names = "--port", description = "the network port where the service must be published", defaultValue="8001")
	private int port;

	@Option(names = "--dir", description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	/**
	 * Fills the consensus configuration, either from the explicit configuration file or
	 * from defaults. In any case, applies the specific updates contained in the options of this command.
	 * 
	 * @param builder the builder that will get enriched with the parameters of the consensus configuration
	 * @throws CommandException if the consensus configuration cannot built
	 */
	protected void fillConsensusNodeConfig(ConsensusConfigBuilder<?,?> builder) throws CommandException {
		try {
			if (openUnsignedFaucet != null)
				builder = builder.allowUnsignedFaucet(openUnsignedFaucet);

			if (signature != null)
				builder = builder.setSignatureForRequests(signature);

			if (initialGasPrice != null)
				builder = builder.setInitialGasPrice(initialGasPrice);

			if (oblivion != null)
				builder = builder.setOblivion(oblivion);

			if (ignoreGasPrice != null)
				builder = builder.ignoreGasPrice(ignoreGasPrice);

			if (chainId != null)
				builder = builder.setChainId(chainId);

			if (initialSupply != null)
				builder = builder.setInitialSupply(initialSupply);

			if (finalSupply != null)
				builder = builder.setFinalSupply(finalSupply);

			if (publicKeyOfGamete != null)
				builder = builder.setPublicKeyOfGamete(mkPublicKeyOfGamete(signature != null ? signature : builder.build().getSignatureForRequests()));
		}
		catch (InvalidKeyException e) {
			throw new CommandException("The public key is invalid for the selected signature algorithm", e);
		}
	}

	private PublicKey mkPublicKeyOfGamete(SignatureAlgorithm signature) throws CommandException {
		try {
			return signature.publicKeyFromEncoding(Base58.fromBase58String(publicKeyOfGamete));
		}
		catch (Base58ConversionException e) {
			throw new CommandException("The public key of the gamete is not in Base58 format", e);
		}
		catch (InvalidKeySpecException e) {
			throw new CommandException("The public key of the gamete is not valid for the " + signature + " signature algorithm");
		}
	}

	/**
	 * Asks the user about the real intention to proceed with the destruction of the given directory.
	 * 
	 * @param dir the directory to delete
	 * @throws CommandException if the user replies negatively
	 */
	protected final void askForConfirmation(Path dir) throws CommandException {
		if (!yes && !json && !answerIsYes(asInteraction("Do you really want to start a new node at \"" + dir + "\" (old blocks and store will be lost) [Y/N] ")))
			throw new CommandException("Stopped");
	}

	protected boolean json() {
		return json;
	}

	protected Path getTakamakaCode() {
		return takamakaCode;
	}

	protected int getPort() {
		return port;
	}

	protected Path getDir() {
		return dir;
	}

	protected BigInteger getMaxGasPerView() {
		return maxGasPerView;
	}

	/**
	 * The output of this command.
	 */
	protected abstract static class AbstractInitOutput implements NodesInitOutput {
		private final StorageReference gamete;
		private final URI uri;

		protected AbstractInitOutput(StorageReference gamete, URI uri) {
			this.gamete = gamete;
			this.uri = uri;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public AbstractInitOutput(NodesInitOutputJson json) throws InconsistentJsonException {
			this.gamete = Objects.requireNonNull(json.getGamete(), "gamete cannot be null", InconsistentJsonException::new).unmap()
				.asReference(value -> new InconsistentJsonException("The reference to the gamete must be a storage reference, not a " + value.getClass().getName()));
			this.uri = Objects.requireNonNull(json.getURI(), "uri cannot be null", InconsistentJsonException::new);
		}

		@Override
		public final StorageReference getGamete() {
			return gamete;
		}

		@Override
		public final URI getURI() {
			return uri;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			toString(sb);

			sb.append("The owner of the key pair of the gamete can bind it now to its address with:\n");
			sb.append(asCommand("  moka keys bind file_containing_the_key_pair_of_the_gamete --password --url url_of_this_Hotmoka_node\n"));
			sb.append("or with:\n");
			sb.append(asCommand("  moka keys bind file_containing_the_key_pair_of_the_gamete --password --reference " + gamete + "\n"));
			sb.append("\n");
			sb.append(asInteraction("Press the enter key to stop this process and close this node: "));

			return sb.toString();
		}

		/**
		 * Appends data to the given string builder. That data will appear at the beginning of the {@link #toString()} of this output.
		 * 
		 * @param sb the string builder
		 */
		protected abstract void toString(StringBuilder sb);
	}
}