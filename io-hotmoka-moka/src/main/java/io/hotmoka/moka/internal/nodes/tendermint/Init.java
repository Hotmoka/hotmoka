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

package io.hotmoka.moka.internal.nodes.tendermint;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.NodesTendermintInitOutputs;
import io.hotmoka.moka.api.nodes.tendermint.NodesTendermintInitOutput;
import io.hotmoka.moka.api.nodes.tendermint.NodesTendermintInitOutput.ValidatorDescription;
import io.hotmoka.moka.internal.AbstractNodeInit;
import io.hotmoka.moka.internal.converters.TendermintNodeConfigOptionConverter;
import io.hotmoka.moka.internal.converters.ValidatorsConsensusConfigOptionConverter;
import io.hotmoka.moka.internal.json.NodesTendermintInitOutputJson;
import io.hotmoka.moka.internal.json.NodesTendermintInitOutputJson.ValidatorDescriptionJson;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ValidatorsConsensusConfig;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.tendermint.TendermintInitializedNodes;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.websockets.api.FailedDeploymentException;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "init",
	header = "Initialize a new Tendermint node and publish a service to it.",
	description = "This command spawns and initializes both a Tendermint engine and a Tendermint node on top of that engine. The configurations of both can be provided through the --tendermint-config and (--local-config and --consensus-config), respectively, which, when missing, rely on defaults. In any case, such configurations can be updated with explicit values.",
	showDefaultValues = true)
public class Init extends AbstractNodeInit {

	@Option(names = "--tendermint-config", paramLabel = "<path>", description = "the directory containing the configuration of the underlying Tendermint engine; this is a directory containing config/ and data/ that can be generated, for instance, by the tendermint init command of Tendermint; if missing, a default configuration for a one-validator network will be used; this will be copied inside the directory specified by --chain-dir")
	private Path tendermintConfig;

	@Option(names = "--local-config", paramLabel = "<path>", description = "the local configuration of the Hotmoka node, in TOML format", converter = TendermintNodeConfigOptionConverter.class)
	private TendermintNodeConfig localConfig;

	@Option(names = "--consensus-config", paramLabel = "<path>", description = "the consensus configuration of the Hotmoka network, in TOML format", converter = ValidatorsConsensusConfigOptionConverter.class)
	private ValidatorsConsensusConfig<?, ?> consensusConfig;

	@Option(names = "--percent-staked", description = "amount of validators' rewards that gets staked; the rest is sent to the validators immediately (0 = 0%%, 1000000 = 1%%)")
	private Integer percentStaked;

	@Option(names = "--buyer-surcharge", description = "extra tax paid when a validator acquires the shares of another validator (in percent of the offer cost) (0 = 0%%, 1000000 = 1%%)")
	private Integer buyerSurcharge;

	@Option(names = "--slashing-for-misbehaving", description = "the percent of stake that gets slashed for misbehaving validators (0 = 0%%, 1000000 = 1%%)")
	private Integer slashingForMisbehaving;

	@Option(names = "--slashing-for-not-behaving", description = "the percent of stake that gets slashed for validators that do not behave (or do not vote) (0 = 0%%, 1000000 = 1%%)")
	private Integer slashingForNotBehaving;

	@Option(names = "--delete-tendermint-config", description = "delete the directory specified by --tendermint-config after starting the node and copying it inside the directory specified by --chain-dir")
	private boolean deleteTendermintConfig;

	@Option(names = "--bind-validators", description = "bind the key pair files of the validators to their storage references, if they exist; this requires the key pairs of the validators to be in files named as their public key, base58 encoded, with suffix .pem")
	private boolean bindValidators;

	@Override
	protected void execute() throws CommandException {
		TendermintNodeConfig localNodeConfig = mkLocalConfig();
		ValidatorsConsensusConfig<?, ?> consensus = mkConsensusConfig();
		askForConfirmation(localNodeConfig.getDir());

		try (var node = TendermintNodes.init(localNodeConfig);
			 var initialized = TendermintInitializedNodes.of(node, consensus, getTakamakaCode());
			 var service = NodeServices.of(node, getPort())) {

			cleanUp();
			var output = new Output(initialized.gamete(), URI.create("ws://localhost:" + getPort()), scanValidators(node));
			report(json(), output, NodesTendermintInitOutputs.Encoder::new);

			waitForEnterKey();
		}
		catch (FailedDeploymentException e) {
			throw new CommandException("Cannot deploy the service at port " + getPort());
		}
		catch (IOException e) {
			throw new CommandException("Cannot access file \"" + getTakamakaCode() + "\"!", e);
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new CommandException("Could not initialize the node", e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CommandException("The operation has been interrupted", e);
		}
		catch (NodeException e) {
			throw new RuntimeException(e);
		}
		catch (TimeoutException e) {
			throw new CommandException("The operation has timed-out", e);
		}
	}

	/**
	 * Yields the local configuration of the Hotmoka node.
	 * 
	 * @return the local configuration of the Hotmoka node
	 * @throws CommandException if the configuration cannot be built
	 */
	private TendermintNodeConfig mkLocalConfig() throws CommandException {
		var builder = localConfig != null ? localConfig.toBuilder() : TendermintNodeConfigBuilders.defaults();

		if (getMaxGasPerView() != null)
			builder = builder.setMaxGasPerViewTransaction(getMaxGasPerView());

		if (getChainDir() != null)
			builder = builder.setDir(getChainDir());

		if (tendermintConfig != null)
			builder.setTendermintConfigurationToClone(tendermintConfig);

		return builder.build();
	}

	private ValidatorsConsensusConfig<?, ?> mkConsensusConfig() throws CommandException {
		try {
			var builder = consensusConfig != null ? consensusConfig.toBuilder() : ValidatorsConsensusConfigBuilders.defaults();
			fillConsensusConfig(builder);

			if (percentStaked != null)
				builder = builder.setPercentStaked(percentStaked);

			if (buyerSurcharge != null)
				builder = builder.setBuyerSurcharge(buyerSurcharge);

			if (slashingForMisbehaving != null)
				builder = builder.setSlashingForMisbehaving(slashingForMisbehaving);

			if (slashingForNotBehaving != null)
				builder = builder.setSlashingForNotBehaving(slashingForNotBehaving);

			return builder.build();
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("A cyrptographic algorithm is not available", e);
		}
	}

	/**
	 * Scans the validators, collecting their storage reference and public key and binds
	 * them to their address, if required.
	 * 
	 * @param node the node whose validators are getting scanned
	 * @throws NodeException
	 * @throws TimeoutException if some operation times out
	 * @throws InterruptedException if some operation gets interrupted while waiting for its termination
	 * @throws CommandException if the scan failed or some validators has a wrong key
	 */
	private SortedSet<ValidatorDescription> scanValidators(Node node) throws NodeException, TimeoutException, InterruptedException, CommandException {
		var takamakaCode = node.getTakamakaCode();
		var manifest = node.getManifest();
		var result = new TreeSet<ValidatorDescription>();
	
		try {
			var validators = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_VALIDATORS + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_VALIDATORS, CommandException::new);
			var shares = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_SHARES, validators))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_SHARES + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_SHARES, CommandException::new);
			int numOfValidators = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SIZE, shares))
					.orElseThrow(() -> new CommandException(MethodSignatures.STORAGE_MAP_VIEW_SIZE + " should not return void"))
					.asReturnedInt(MethodSignatures.STORAGE_MAP_VIEW_SIZE, CommandException::new);
	
			for (int num = 0; num < numOfValidators; num++) {
				var validator = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SELECT, shares, StorageValues.intOf(num)))
						.orElseThrow(() -> new CommandException(MethodSignatures.STORAGE_MAP_VIEW_SELECT + " should not return void"))
						.asReturnedReference(MethodSignatures.STORAGE_MAP_VIEW_SELECT, CommandException::new);
				String publicKeyBase64 = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, validator))
						.orElseThrow(() -> new NodeException(MethodSignatures.PUBLIC_KEY + " should not return void"))
						.asReturnedString(MethodSignatures.PUBLIC_KEY, CommandException::new);
	
				String publicKeyBase58;
				try {
					publicKeyBase58 = Base58.toBase58String(Base64.fromBase64String(publicKeyBase64));
				}
				catch (Base64ConversionException e) {
					throw new CommandException("The public key of validator " + validator + " is not in Base64 format", e);
				}
	
				result.add(new ValidatorDescriptionImpl(validator, publicKeyBase58));
	
				if (bindValidators) {
					// the pem file, if it exists, is named with the public key, base58
					var path = Paths.get(publicKeyBase58 + ".pem");
					try {
						var entropy = Entropies.load(path);
						var account = Accounts.of(entropy, validator);
						var pathOfAccount = getChainDir().resolve(account + ".pem");
						account.dump(pathOfAccount);
						Files.delete(path);
					}
					catch (IOException e) {
						throw new CommandException("Could not bind the key pair of validator " + validator + " to its reference: its key pair was expected to be in file " + asPath(path));
					}
				}
			}

			return result;
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new CommandException("Could not scan the validators of the new node", e);
		}
	}

	private void cleanUp() throws CommandException {
		try {
			if (deleteTendermintConfig)
				Files.walk(tendermintConfig)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
		}
		catch (IOException e) {
			throw new CommandException("Cannot delete the Tendermint configuration", e);
		}
	}

	/**
	 * Implementation of the description of a validator.
	 */
	public static class ValidatorDescriptionImpl implements ValidatorDescription {
		private final StorageReference reference;
		private final String publicKeyBase58;

		private ValidatorDescriptionImpl(StorageReference reference, String publicKeyBase58) {
			this.reference = reference;
			this.publicKeyBase58 = publicKeyBase58;
		}

		/**
		 * Builds a validator description from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public ValidatorDescriptionImpl(ValidatorDescriptionJson json) throws InconsistentJsonException {
			this.reference = Objects.requireNonNull(json.getReference(), "reference cannot be null", InconsistentJsonException::new)
				.unmap().asReference(value -> new InconsistentJsonException("reference should be a storage reference, not a " + value.getClass().getName()));
			this.publicKeyBase58 = Objects.requireNonNull(json.getPublicKeyBase58(), "publicKeyBase58 cannot be null", InconsistentJsonException::new);
		}

		@Override
		public StorageReference getReference() {
			return reference;
		}

		@Override
		public String getPublicKeyBase58() {
			return publicKeyBase58;
		}

		@Override
		public int compareTo(ValidatorDescription other) {
			return reference.compareTo(other.getReference());
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof ValidatorDescription vd && reference.equals(vd.getReference());
		}

		@Override
		public int hashCode() {
			return reference.hashCode();
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractNodeInitOutput implements NodesTendermintInitOutput {
		private final SortedSet<ValidatorDescription> validators;

		private Output(StorageReference gamete, URI uri, SortedSet<ValidatorDescription> validators) {
			super(gamete, uri);

			this.validators = validators;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesTendermintInitOutputJson json) throws InconsistentJsonException {
			super(json);

			this.validators = new TreeSet<>();
			for (var validatorDescriptionJson: json.getValidators().toArray(ValidatorDescriptionJson[]::new))
				this.validators.add(Objects.requireNonNull(validatorDescriptionJson, "validators cannot hold null elements", InconsistentJsonException::new).unmap());
		}

		@Override
		public Stream<ValidatorDescription> getValidators() {
			return validators.stream();
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			sb.append("The following service has been published:\n");
			sb.append(" * " + asUri(getURI()) + ": the API of this Hotmoka node\n");
			sb.append("\n");
			sb.append("The validators are the following accounts:\n");
			validators.forEach(validator -> sb.append(" * " + validator.getReference() + " with public key " + validator.getPublicKeyBase58() + " (ed25519, base58)\n"));
			sb.append("\n");

			toStringNodeInit(sb);

			return sb.toString();
		}
	}
}