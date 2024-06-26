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

package io.hotmoka.moka.internal;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.helpers.ManifestHelpers;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.tendermint.TendermintInitializedNodes;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import io.takamaka.code.constants.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "init-tendermint",
	description = "Initialize a new node based on Tendermint",
	showDefaultValues = true)
public class InitTendermint extends AbstractCommand {

	@Parameters(description = "the initial supply of coins of the node, which goes to the gamete")
    private BigInteger initialSupply;

	private final static String DELTA_SUPPLY_DEFAULT = "equal to the initial supply";
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

	@Option(names = { "--port" }, description = "the network port for the publication of the service", defaultValue="8001")
	private int port;

	@Option(names = { "--dir" }, description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = { "--takamaka-code" }, description = "the jar with the basic Takamaka classes that will be installed in the node",
			defaultValue = "io-hotmoka-moka/modules/explicit/io-takamaka-code-TAKAMAKA-VERSION.jar")
	private String takamakaCode;

	@Option(names = { "--tendermint-config" }, description = "the directory of the Tendermint configuration of the node", defaultValue = "io-hotmoka-moka/tendermint_configs/v1n0/node0")
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
		private final InitializedNode initialized;

		private Run() throws Exception {
			checkPublicKey(keyOfGamete);
			askForConfirmation();

			var nodeConfig = TendermintNodeConfigBuilders.defaults()
				.setTendermintConfigurationToClone(tendermintConfig)
				.setMaxGasPerViewTransaction(maxGasPerView)
				.setDir(dir)
				.build();

			BigInteger deltaSupply;
			if (DELTA_SUPPLY_DEFAULT.equals(InitTendermint.this.deltaSupply))
				deltaSupply = initialSupply;
			else
				deltaSupply = new BigInteger(InitTendermint.this.deltaSupply);

			var signature = SignatureAlgorithms.ed25519();

			var consensus = ValidatorsConsensusConfigBuilders.defaults()
				.allowUnsignedFaucet(openUnsignedFaucet)
				.ignoreGasPrice(ignoreGasPrice)
				.setSignatureForRequests(signature)
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
				.setPublicKeyOfGamete(signature.publicKeyFromEncoding(Base58.decode(keyOfGamete)))
				.build();

			try (var node = TendermintNodes.init(nodeConfig);
				 var initialized = this.initialized = TendermintInitializedNodes.of(node, consensus, Paths.get(takamakaCode.replace("TAKAMAKA-VERSION", Constants.TAKAMAKA_VERSION)));
				 var service = NodeServices.of(initialized, port)) {

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
				var takamakaCode = initialized.getTakamakaCode();
				var manifest = initialized.getManifest();
				var validators = (StorageReference) initialized.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest))
					.orElseThrow(() -> new NodeException(MethodSignatures.GET_VALIDATORS + " should not return void"));
				var shares = (StorageReference) initialized.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_VIEW, "getShares", StorageTypes.STORAGE_MAP_VIEW), validators))
					.orElseThrow(() -> new NodeException("getShares() should not return void"));
				int numOfValidators = ((IntValue) initialized.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "size", StorageTypes.INT), shares))
					.orElseThrow(() -> new NodeException("size() should not return void"))).getValue();

				for (int num = 0; num < numOfValidators; num++) {
					var validator = (StorageReference) initialized.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "select", StorageTypes.OBJECT, StorageTypes.INT), shares, StorageValues.intOf(num)))
						.orElseThrow(() -> new NodeException("select() should not return void"));
					String publicKeyBase64 = ((StringValue) initialized.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, validator))
						.orElseThrow(() -> new NodeException(MethodSignatures.PUBLIC_KEY + " should not return void"))).getValue();
					String publicKeyBase58 = Base58.encode(Base64.fromBase64String(publicKeyBase64));
					// the pem file, if it exists, is named with the public key, base58
					try {
						var path = Paths.get(publicKeyBase58 + ".pem");
						var entropy = Entropies.load(path);
						var account = Accounts.of(entropy, validator);
						var pathOfAccount = dir.resolve(account + ".pem");
						account.dump(pathOfAccount);
						Files.delete(path);
						System.out.println("The entropy of the validator #" + num + " has been saved into the file \"" + pathOfAccount + "\".");
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
			System.out.println("The node has been published at ws://localhost:" + port);
		}

		private void printManifest() throws TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
			System.out.println("\nThe following node has been initialized:\n" + ManifestHelpers.of(initialized));
		}

		private void dumpInstructionsToBindGamete() throws NodeException, TimeoutException, InterruptedException {
			System.out.println("\nThe owner of the key of the gamete can bind it to its address now:");
			System.out.println("  moka bind-key " + keyOfGamete + " --url url_of_this_node");
			System.out.println("or");
			System.out.println("  moka bind-key " + keyOfGamete + " --reference " + initialized.gamete() + "\n");
		}
	}
}