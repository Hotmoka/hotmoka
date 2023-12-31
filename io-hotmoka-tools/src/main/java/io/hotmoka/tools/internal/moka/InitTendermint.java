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

package io.hotmoka.tools.internal.moka;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Comparator;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.StorageTypes;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.constants.Constants;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.helpers.ManifestHelpers;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.SimpleValidatorsConsensusConfigBuilders;
import io.hotmoka.node.service.NodeServiceConfigBuilders;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.service.api.NodeServiceConfig;
import io.hotmoka.node.tendermint.TendermintInitializedNodes;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "init-tendermint",
	description = "Initialize a new node based on Tendermint",
	showDefaultValues = true)
public class InitTendermint extends AbstractCommand {

	@Parameters(description = "the initial supply of coins of the node, which goes to the gamete")
    private BigInteger initialSupply;

	private final static String DELTA_SUPPLY_DEFAULT = "equals to the initial supply";
	@Option(names = { "--delta-supply" }, description = "the amount of coins that can be minted during the life of the node, after which inflation becomes 0", defaultValue = DELTA_SUPPLY_DEFAULT)
    private String deltaSupply;

	@Option(names = { "--initial-red-supply" }, description = "the initial supply of red coins of the node, which goes to the gamete", defaultValue = "0")
    private BigInteger initialRedSupply;

	@Option(names = { "--key-of-gamete" }, description = "the Base58-encoded public key of the gamete account")
    private String keyOfGamete;

	@Option(names = { "--open-unsigned-faucet" }, description = "opens the unsigned faucet of the gamete") 
	private boolean openUnsignedFaucet;

	@Option(names = { "--ignore-gas-price" }, description = "accepts transactions regardless of their gas price") 
	private boolean ignoreGasPrice;

	@Option(names = { "--max-gas-per-view" }, description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Option(names = { "--initial-gas-price" }, description = "the initial price of a unit of gas", defaultValue = "100") 
	private BigInteger initialGasPrice;

	@Option(names = { "--oblivion" }, description = "how quick the gas consumed at previous rewards is forgotten (0 = never, 1000000 = immediately). Use 0 to keep the gas price constant", defaultValue = "250000") 
	private long oblivion;

	@Option(names = { "--inflation" }, description = "inflation added to the remuneration of the validators at each block (0 = 0%, 1000000 = 1%)", defaultValue = "1000000")
	private long inflation;

	@Option(names = { "--percent-staked" }, description = "amount of validators' rewards that gets staked, the rest is sent to the validators immediately (0 = 0%, 1000000 = 1%)", defaultValue = "75000000")
	private int percentStaked;

	@Option(names = { "--buyer-surcharge" }, description = "extra tax paid when a validator acquires the shares of another validator (in percent of the offer cost) (0 = 0%, 1000000 = 1%)", defaultValue = "50000000")
	private int buyerSurcharge;

	@Option(names = { "--slashing-for-misbehaving" }, description = "the percent of stake that gets slashed for each misbehaving validator (0 = 0%, 1000000 = 1%)", defaultValue = "1000000")
	private int slashingForMisbehaving;

	@Option(names = { "--slashing-for-not-behaving" }, description = "the percent of stake that gets slashed for validators that do not behave (or do not vote) (0 = 0%, 1000000 = 1%)", defaultValue = "500000")
	private int slashingForNotBehaving;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true")
	private boolean interactive;

	@Option(names = { "--port" }, description = "the network port for the publication of the service", defaultValue="8080")
	private int port;

	@Option(names = { "--dir" }, description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = { "--takamaka-code" }, description = "the jar with the basic Takamaka classes that will be installed in the node",
			defaultValue = "modules/explicit/io-takamaka-code-TAKAMAKA-VERSION.jar")
	private String takamakaCode;

	@Option(names = { "--tendermint-config" }, description = "the directory of the Tendermint configuration of the node", defaultValue = "io-hotmoka-tools/tendermint_configs/v1n0/node0")
	private Path tendermintConfig;

	@Option(names = { "--delete-tendermint-config" }, description = "delete the directory of the Tendermint configuration after starting the node")
	private boolean deleteTendermintConfig;

	@Option(names = { "--bind-validators" }, description = "bind the pem of the keys of the validators, if they exist")
	private boolean bindValidators;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final NodeServiceConfig networkConfig;
		private final InitializedNode initialized;

		private Run() throws Exception {
			checkPublicKey(keyOfGamete);
			askForConfirmation();

			var nodeConfig = TendermintNodeConfigBuilders.defaults()
				.setTendermintConfigurationToClone(tendermintConfig)
				.setMaxGasPerViewTransaction(maxGasPerView)
				.setDir(dir)
				.build();

			networkConfig = NodeServiceConfigBuilders.defaults()
				.setPort(port)
				.build();

			BigInteger deltaSupply;
			if (DELTA_SUPPLY_DEFAULT.equals(InitTendermint.this.deltaSupply))
				deltaSupply = initialSupply;
			else
				deltaSupply = new BigInteger(InitTendermint.this.deltaSupply);

			var consensus = SimpleValidatorsConsensusConfigBuilders.defaults()
				.allowUnsignedFaucet(openUnsignedFaucet)
				.ignoreGasPrice(ignoreGasPrice)
				.setInitialGasPrice(initialGasPrice)
				.setOblivion(oblivion)
				.setPercentStaked(percentStaked)
				.setBuyerSurcharge(buyerSurcharge)
				.setSlashingForMisbehaving(slashingForMisbehaving)
				.setSlashingForNotBehaving(slashingForNotBehaving)
				.setInitialInflation(inflation)
				.setInitialSupply(initialSupply)
				.setFinalSupply(initialSupply.add(deltaSupply))
				.setInitialRedSupply(initialRedSupply)
				.setPublicKeyOfGamete(Base64.getEncoder().encodeToString(Base58.decode(keyOfGamete)))
				.build();

			try (var node = TendermintNodes.init(nodeConfig, consensus);
				 var initialized = this.initialized = TendermintInitializedNodes.of(node, consensus, Paths.get(takamakaCode.replace("TAKAMAKA-VERSION", Constants.TAKAMAKA_VERSION)));
				 var service = NodeServices.of(networkConfig, initialized)) {

				bindValidators();
				cleanUp();
				printManifest();
				printBanner();
				dumpInstructionsToBindGamete();
				waitForEnterKey();
			}
		}

		private void bindValidators() throws Exception {
			if (bindValidators) {
				TransactionReference takamakaCode = initialized.getTakamakaCode();
				StorageReference manifest = initialized.getManifest();
				var validators = (StorageReference) initialized.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.GET_VALIDATORS, manifest));
				var shares = (StorageReference) initialized.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(StorageTypes.SHARED_ENTITY_VIEW, "getShares", StorageTypes.STORAGE_MAP_VIEW), validators));
				int numOfValidators = ((IntValue) initialized.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(StorageTypes.STORAGE_MAP_VIEW, "size", BasicTypes.INT), shares))).value;
				for (int num = 0; num < numOfValidators; num++) {
					var validator = (StorageReference) initialized.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
						(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(StorageTypes.STORAGE_MAP_VIEW, "select", StorageTypes.OBJECT, BasicTypes.INT), shares, new IntValue(num)));
					String publicKeyBase64 = ((StringValue) initialized.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
						(manifest, _100_000, takamakaCode, CodeSignature.PUBLIC_KEY, validator))).value;
					String publicKeyBase58 = Base58.encode(Base64.getDecoder().decode(publicKeyBase64));
					// the pem file, if it exists, is named with the public key, base58
					try {
						var entropy = Entropies.load(Paths.get(publicKeyBase58 + ".pem"));
						Path fileName = Accounts.of(entropy, validator).dump(dir);
						entropy.delete(publicKeyBase58);
						System.out.println("The entropy of the validator #" + num + " has been saved into the file \"" + fileName + "\".");
					}
					catch (IOException e) {
						System.out.println("Could not bind the key of validator #" + num + "!");
					}
				}
			}
		}

		private void cleanUp() throws IOException {
			if (deleteTendermintConfig)
				Files.walk(tendermintConfig)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}

		private void askForConfirmation() {
			if (interactive)
				yesNo("Do you really want to start a new node at \"" + dir + "\" (old blocks and store will be lost) [Y/N] ");
		}

		private void waitForEnterKey() throws IOException {
			System.out.println("Press enter to exit this program and turn off the node");
			System.in.read();
		}

		private void printBanner() {
			System.out.println("The node has been published at localhost:" + networkConfig.getPort());
			System.out.println("Try for instance in a browser: http://localhost:" + networkConfig.getPort() + "/get/manifest");
		}

		private void printManifest() throws TransactionRejectedException, TransactionException, CodeExecutionException {
			System.out.println("\nThe following node has been initialized:\n" + ManifestHelpers.of(initialized));
		}

		private void dumpInstructionsToBindGamete() {
			System.out.println("\nThe owner of the key of the gamete can bind it to its address now:");
			System.out.println("  moka bind-key " + keyOfGamete + " --url url_of_this_node");
			System.out.println("or");
			System.out.println("  moka bind-key " + keyOfGamete + " --reference " + initialized.gamete() + "\n");
		}
	}
}