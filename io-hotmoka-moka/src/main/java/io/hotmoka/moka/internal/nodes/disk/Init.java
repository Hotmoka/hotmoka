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

package io.hotmoka.moka.internal.nodes.disk;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;

import io.hotmoka.cli.AbstractCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base58ConversionException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.DiskNodes;
import io.hotmoka.node.service.NodeServices;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "init",
	description = "Initialize a new disk node and publish a service to it.",
	showDefaultValues = true)
public class Init extends AbstractCommand {

	@Parameters(description = "the initial supply of coins of the node, which goes to the gamete")
    private BigInteger initialSupply;

	@Parameters(description = "the Base58-encoded ed25519 public key to use for the gamete account")
    private String publicKeyOfGamete;

	@Parameters(description = "the path of the jar with the basic Takamaka classes that will be installed in the node")
	private Path takamakaCode;

	@Option(names = "--delta-supply", description = "the amount of coins that can be minted during the life of the node, after which inflation becomes 0", defaultValue = "0")
    private BigInteger deltaSupply;

	@Option(names = "--chain-id", description = "the chain identifier of the network", defaultValue = "")
	private String chainId;

	@Option(names = "--signature", description = "the default signature algorithm to use for signing the requests to the node (ed25519, sha256dsa, qtesla1, qtesla3)",
			converter = SignatureOptionConverter.class, defaultValue = "ed25519")
	private SignatureAlgorithm signature;
	
	@Option(names = "--open-unsigned-faucet", description = "opens the unsigned faucet of the gamete") 
	private boolean openUnsignedFaucet;

	@Option(names = "--initial-gas-price", description = "the initial price of a unit of gas", defaultValue = "100") 
	private BigInteger initialGasPrice;

	@Option(names = "--oblivion", description = "how quick the gas consumed at previous rewards is forgotten (0 = never, 1000000 = immediately); use 0 to keep the gas price constant", defaultValue = "250000") 
	private long oblivion;

	@Option(names = "--ignore-gas-price", description = "accept transactions regardless of their gas price") 
	private boolean ignoreGasPrice;

	@Option(names = "--max-gas-per-view", description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Option(names = "--yes", description = "assume yes when asked for confirmation")
	private boolean yes;

	@Option(names = "--port", description = "the network port where the service must be published", defaultValue="8001")
	private int port;

	@Option(names = "--dir", description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	@Override
	protected void execute() throws CommandException {
		askForConfirmation();

		var nodeConfig = DiskNodeConfigBuilders.defaults()
			.setMaxGasPerViewTransaction(maxGasPerView)
			.setDir(dir)
			.build();

		PublicKey publicKey;

		try {
			publicKey = signature.publicKeyFromEncoding(Base58.fromBase58String(publicKeyOfGamete));
		}
		catch (Base58ConversionException e) {
			throw new CommandException("The public key of the gamete is not in Base58 format", e);
		}
		catch (InvalidKeySpecException e) {
			throw new CommandException("The public key is not valid for the " + signature + " signature algorithm");
		}
			
		ConsensusConfig<?, ?> consensus;

		try {
			consensus = ConsensusConfigBuilders.defaults(signature)
				.allowUnsignedFaucet(openUnsignedFaucet)
				.setInitialGasPrice(initialGasPrice)
				.setSignatureForRequests(signature)
				.setOblivion(oblivion)
				.ignoreGasPrice(ignoreGasPrice)
				.setChainId(chainId)
				.setInitialSupply(initialSupply)
				.setFinalSupply(initialSupply.add(deltaSupply))
				.setPublicKeyOfGamete(publicKey)
				.build();
		}
		catch (InvalidKeyException e) {
			// this should not happen since we have created the public key with the same signature algorithm
			throw new RuntimeException(e);
		}

		try (var node = DiskNodes.init(nodeConfig);
			var initialized = InitializedNodes.of(node, consensus, takamakaCode);
			var service = NodeServices.of(node, port)) {

			StorageReference gamete = initialized.gamete();

			if (json)
				System.out.println(new Gson().toJson(new Result(gamete)));
			else {
				System.out.println("The node has been published at ws://localhost:" + port + "\n");
				System.out.println("The owner of the key of the gamete can bind it to its address now with:");
				System.out.println("  moka accounts create-from-keys file_containing_the_key_pair_of_the_gamete --password --url url_of_this_node");
				System.out.println("or with");
				System.out.println("  moka accounts create-from-keys file_containing_the_key_pair_of_the_gamete --password --reference " + gamete);
				System.out.println("\nPress the enter key to stop the process and close the node");
			}

			waitForEnterKey();
		}
		catch (IOException e) {
			throw new CommandException("Cannot access " + takamakaCode, e);
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

	private void askForConfirmation() throws CommandException {
		if (!yes && !answerIsYes("Do you really want to start a new node at \"" + dir + "\" (old blocks and store will be lost) [Y/N] "))
			throw new CommandException("Stopped");
	}

	private static class Result {

		@SuppressWarnings("unused")
		private final String gamete;

		private Result(StorageReference gamete) {
			this.gamete = gamete.toString();
		}
	}
}