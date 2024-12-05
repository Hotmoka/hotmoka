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

package io.hotmoka.tests;

/*
 * MODIFY AT LINE 153 TO SELECT THE NODE IMPLEMENTATION TO TEST.
 */

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.AccountsNodes;
import io.hotmoka.helpers.Coin;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.JarsNodes;
import io.hotmoka.helpers.api.AccountsNode;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.ConstructorFuture;
import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.MethodFuture;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.ConsensusConfigBuilder;
import io.hotmoka.node.api.nodes.ValidatorsConsensusConfig;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.DiskNodes;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.mokamint.MokamintNodeConfigBuilders;
import io.hotmoka.node.mokamint.MokamintNodes;
import io.hotmoka.node.mokamint.api.MokamintNode;
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.tendermint.TendermintInitializedNodes;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import io.hotmoka.node.tendermint.api.TendermintNode;
import io.hotmoka.testing.AbstractLoggedTests;
import io.hotmoka.verification.VerificationException;
import io.mokamint.miner.api.Miner;
import io.mokamint.miner.local.LocalMiners;
import io.mokamint.node.Peers;
import io.mokamint.node.api.PeerRejectedException;
import io.mokamint.node.local.LocalNodeConfigBuilders;
import io.mokamint.node.service.PublicNodeServices;
import io.mokamint.nonce.Prologs;
import io.mokamint.plotter.PlotAndKeyPairs;
import io.mokamint.plotter.Plots;
import io.mokamint.plotter.api.Plot;
import io.mokamint.plotter.api.PlotAndKeyPair;
import io.takamaka.code.constants.Constants;
import jakarta.websocket.DeploymentException;

@ExtendWith(HotmokaTest.NodeHandler.class)
public abstract class HotmokaTest extends AbstractLoggedTests {
	protected static final BigInteger _50_000 = BigInteger.valueOf(50_000);
	protected static final BigInteger _100_000 = BigInteger.valueOf(100_000);
	protected static final BigInteger _500_000 = BigInteger.valueOf(500_000);
	protected static final BigInteger _1_000_000 = BigInteger.valueOf(1_000_000);
	protected static final BigInteger _10_000_000 = BigInteger.valueOf(10_000_000);
	protected static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);
	protected static final BigInteger _10_000_000_000 = BigInteger.valueOf(10_000_000_000L);

	static class NodeHandler implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

	    private static boolean started = false;

	    @Override
	    public void beforeAll(ExtensionContext context) throws Exception {
	    	if (!started) {
	    		started = true;
	    		context.getRoot().getStore(org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL).put("any unique name", this);

	    		// we use always the same entropy and password, so that the tests become deterministic (if they are not explicitly non-deterministic)
	    		var entropy = Entropies.of(new byte[16]);
	    		var password = "";
	    		var localSignature = signature = SignatureAlgorithms.ed25519det();  // good for testing
	    		var keys = entropy.keys(password, localSignature);
	    		consensus = ValidatorsConsensusConfigBuilders.defaults()
	    				.setSignatureForRequests(signature)
	    				.allowUnsignedFaucet(true) // good for testing
	    				.ignoreGasPrice(true) // good for testing
	    				.setInitialSupply(Coin.level7(10000000)) // enough for all tests
	    				.setInitialRedSupply(Coin.level7(10000000)) // enough for all tests
	    				.setPublicKeyOfGamete(keys.getPublic())
	    				.build();
	    		privateKeyOfGamete = keys.getPrivate();

	    		Node wrapped;
	    		//node = wrapped = mkDiskNode();
	    		//node = wrapped = mkMokamintNodeConnectedToPeer();
	    		//node = wrapped = mkMokamintNetwork(4);
	    		//node = wrapped = mkTendermintNode();
	    		node = mkRemoteNode(wrapped = mkDiskNode());
	    		//node = mkRemoteNode(wrapped = mkMokamintNetwork(1));
	    		//node = mkRemoteNode(wrapped = mkTendermintNode());
	    		//node = wrapped = mkRemoteNode("ec2-54-194-239-91.eu-west-1.compute.amazonaws.com:8080");
	    		//node = wrapped = mkRemoteNode("localhost:8080");
	    		initializeNodeIfNeeded(wrapped);

	    		manifest = node.getManifest();
	    		takamakaCode = node.getTakamakaCode();

	    		var gamete = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
	    				(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
	    				.orElseThrow(() -> new NodeException(MethodSignatures.GET_GAMETE + " should not return void"))
	    				.asReturnedReference(MethodSignatures.GET_GAMETE, NodeException::new);

	    		chainId = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
	    				(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))
	    				.orElseThrow(() -> new NodeException(MethodSignatures.GET_CHAIN_ID + " should not return void"))
	    				.asReturnedString(MethodSignatures.GET_CHAIN_ID, NodeException::new);

	    		BigInteger nonce = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
	    				(gamete, _100_000, takamakaCode, MethodSignatures.NONCE, gamete))
	    				.orElseThrow(() -> new NodeException(MethodSignatures.NONCE + " should not return void"))
	    				.asReturnedBigInteger(MethodSignatures.NONCE, NodeException::new);

	    		BigInteger aLot = Coin.level6(1000000000);

	    		// we set the thresholds for the faucets of the gamete
	    		Signer<SignedTransactionRequest<?>> signerOfGamete = signature.getSigner(privateKeyOfGamete, SignedTransactionRequest::toByteArrayWithoutSignature);
	    		node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
	    				(signerOfGamete, gamete, nonce, chainId, _100_000, BigInteger.ONE, takamakaCode,
	    						MethodSignatures.ofVoid(StorageTypes.GAMETE, "setMaxFaucet", StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER), gamete,
	    						StorageValues.bigIntegerOf(aLot), StorageValues.bigIntegerOf(aLot)));

	    		var local = AccountsNodes.ofGreenRed(node, gamete, privateKeyOfGamete, aLot, aLot);
	    		localGamete = local.account(0);
	    		privateKeyOfLocalGamete = local.privateKey(0);

	    		System.out.println("Running all tests against node " + node.getNodeInfo());
	        }
	    }

	    @Override
	    public void close() throws Exception {
	    	node.close(); // better close as well, since it might be a remote node

	    	for (var node: nodes)
	    		node.close();

	    	for (var miner: miners)
	    		miner.close();

	    	for (var plot: plots)
	    		plot.close();

	    	if (nodes.size() <= 1)
	    		System.out.println("Closed the test node");
	    	else
	    		System.out.println("Closed the test nodes");
	    }
	}

	/**
	 * The node that gets created before starting to run the tests.
	 * This node will hence be created only once and
	 * each test will create the accounts and add the jars that it needs.
	 */
	protected static Node node;

	/**
	 * The consensus parameters of the node.
	 */
	protected static ConsensusConfig<?,?> consensus;

	/**
	 * The test nodes.
	 */
	private final static List<Node> nodes = new ArrayList<>();

	/**
	 * The plots of the test nodes, if they are Mokamint nodes.
	 */
	private final static List<Plot> plots = new ArrayList<>();

	/**
	 * The miners of the test nodes, if they are Mokamint nodes.
	 */
	private final static List<Miner> miners = new ArrayList<>();

	/**
	 * The private key of the account used at each run of the tests.
	 */
	private static PrivateKey privateKeyOfLocalGamete;

	/**
	 * The account that can be used as gamete for each run of the tests.
	 */
	private static StorageReference localGamete;

	/**
	 * The signature algorithm used for signing the requests.
	 */
	private static SignatureAlgorithm signature;

	/**
	 * The reference to the manifest in the test node.
	 */
	private static StorageReference manifest;

	/**
	 * The transaction that installed the Takamaka runtime in the test node.
	 */
	private static TransactionReference takamakaCode;

	/**
	 * The jar under test.
	 */
	private static TransactionReference jar;

	/**
	 * The node under test. This is a view of {@link #node},
	 * with the addition of some initial accounts, recreated before each test.
	 */
	private AccountsNode nodeWithAccountsView;

	/**
	 * The nonce of each externally owned account used in the test.
	 */
	private final ConcurrentMap<StorageReference, BigInteger> nonces = new ConcurrentHashMap<>();

	/**
	 * The chain identifier of the node used for the tests.
	 */
	private static String chainId;

	/**
	 * The private key of the gamete.
	 */
	private static PrivateKey privateKeyOfGamete;

	public interface TestBody {
		void run() throws Exception;
	}

	private static void initializeNodeIfNeeded(Node node) throws TransactionRejectedException, TransactionException,
			CodeExecutionException, IOException, NodeException, TimeoutException, InterruptedException {

		try {
			node.getManifest();
		}
		catch (NodeException e) {
			// if the original node has no manifest yet, it means that it is not initialized and we initialize it
			// with the Takamaka runtime, that we can find in the Maven repository
			var takamakaCode = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
			if (node instanceof TendermintNode tn)
				TendermintInitializedNodes.of(tn, (ValidatorsConsensusConfig<?,?>) consensus, takamakaCode);
			else
				InitializedNodes.of(node, consensus, takamakaCode);
		}
	}

	@SuppressWarnings("unused")
	private static Node mkTendermintNode() throws NodeException, InterruptedException {
		try {
			consensus = fillConsensusConfig(ValidatorsConsensusConfigBuilders.defaults()).build();

			var config = TendermintNodeConfigBuilders.defaults()
					.setDir(Files.createTempDirectory("hotmoka-tendermint-chain-"))
					.setTendermintConfigurationToClone(Paths.get("tendermint_config"))
					.setMaxGasPerViewTransaction(_10_000_000)
					.build();

			Node node = TendermintNodes.init(config);
			nodes.add(node);
			return node;
		}
		catch (IOException | NoSuchAlgorithmException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * This scenario assumes that there is an external, non-initialized node, published for instance like that:
	 * 
	 * moka start-mokamint --keys CYcdCR4S1zVojhFsB7cxpYsudqBhvRMoXRhFCtwcnUg9.pem --keys-of-plot 5BYtHQ3XaygM7yjJ4vaaftA5AJAC56GNkLrDj4yQ46Wh.pem --plot plot.plot --mokamint-port 8031 --port 8002
	 */
	@SuppressWarnings("unused")
	private static Node mkMokamintNodeConnectedToPeer() throws NodeException, InterruptedException, InvalidKeyException, SignatureException, TimeoutException {
		try {
			consensus = fillConsensusConfig(ValidatorsConsensusConfigBuilders.defaults()).build();

			Path hotmokaChainPath = Files.createTempDirectory("hotmoka-mokamint-chain-");

			var config = MokamintNodeConfigBuilders.defaults()
					.setDir(hotmokaChainPath)
					.setMaxGasPerViewTransaction(_10_000_000)
					.build();

			var mokamintConfig = LocalNodeConfigBuilders.defaults()
					// we use the same chain id for the Hotmoka node and for the underlying Mokamint engine, although this is not necessary
					.setChainId(consensus.getChainId())
					.setTargetBlockCreationTime(2000)
					.setMaximalHistoryChangeTime(300000L) // 5 minutes
					.setDir(hotmokaChainPath.resolve("mokamint")).build();
			var nodeKeys = mokamintConfig.getSignatureForBlocks().getKeyPair();
			var plotKeys = mokamintConfig.getSignatureForDeadlines().getKeyPair();
			var prolog = Prologs.of(mokamintConfig.getChainId(), mokamintConfig.getSignatureForBlocks(), nodeKeys.getPublic(), mokamintConfig.getSignatureForDeadlines(), plotKeys.getPublic(), new byte[0]);
			var plot = Plots.create(hotmokaChainPath.resolve("test.plot"), prolog, 1000, 4000, mokamintConfig.getHashingForDeadlines(), __ -> {});
			plots.add(plot);
			var miner = LocalMiners.of(new PlotAndKeyPair[] { PlotAndKeyPairs.of(plot, plotKeys) });
			miners.add(miner);
			var node = MokamintNodes.init(config, mokamintConfig, nodeKeys, true);
			node.getMokamintNode().add(miner).orElseThrow(() -> new NodeException("Could not add the miner to the test node"));

			NodeServices.of(node, 8001);
			System.out.println("Hotmoka node published at ws://localhost:8001");

			// we open a web service to the underlying Mokamint node, at port 8030; this is not necessary,
			// but it allows developers to query the node during the execution of the tests
			URI uri1 = URI.create("ws://localhost:8030");
			PublicNodeServices.open(node.getMokamintNode(), 8030, 1800000, 1000, Optional.of(uri1));
			System.out.println("Underlying Mokamint node published at " + uri1);

			URI uri2 = URI.create("ws://localhost:8031");

			try {
				if (node.getMokamintNode().add(Peers.of(uri2)).isPresent())
					System.out.println("Added " + uri2 + " as a peer of " + uri1);
				else
					System.out.println("Could not add " + uri2 + " as a peer of " + uri1);
			}
			catch (PeerRejectedException e) {
				System.out.println("Could not add " + uri2 + " as a peer of " + uri1 + ": it has been rejected");
			}

			nodes.add(node);
			return node;
		}
		catch (IOException | NoSuchAlgorithmException | io.mokamint.node.api.NodeException | DeploymentException e) {
			throw new NodeException(e);
		}
	}

	@SuppressWarnings("unused")
	private static Node mkMokamintNetwork(int howManyNodes) throws NodeException, InterruptedException, TimeoutException, TransactionRejectedException, TransactionException, CodeExecutionException {
		if (howManyNodes < 1)
			throw new IllegalArgumentException("A network needs at least a node");

		final var TARGET_BLOCK_CREATION_TIME = 4000;
		final var PLOT_LENGTH = 500L; // TODO
		final var MAX_HISTORY_CHANGE = 5L * 60 * 1000; // five minutes, so that it is possible to see the effects of garbage-collection during the tests

		MokamintNode firstNode = null;
		URI firstUri = null;

		try {
			consensus = fillConsensusConfig(ValidatorsConsensusConfigBuilders.defaults()).build();

			for (int nodeNum = 1; nodeNum <= howManyNodes; nodeNum++) {
				Path hotmokaChainPath = Files.createTempDirectory("hotmoka-mokamint-chain-" + nodeNum + "-");

				var config = MokamintNodeConfigBuilders.defaults()
					.setDir(hotmokaChainPath)
					.setMaxGasPerViewTransaction(_10_000_000)
					.build();

				var mokamintConfig = LocalNodeConfigBuilders.defaults()
					// we use the same chain id for the Hotmoka node and for the underlying Mokamint engine, although this is not necessary
					.setChainId(consensus.getChainId())
					.setTargetBlockCreationTime(TARGET_BLOCK_CREATION_TIME)
					.setMaximalHistoryChangeTime(MAX_HISTORY_CHANGE)
					.setDir(hotmokaChainPath.resolve("mokamint")).build();

				var entropyForNode = Entropies.random();
				KeyPair nodeKeys = entropyForNode.keys("", mokamintConfig.getSignatureForBlocks());
				byte[] nodePublicKeyBytes = mokamintConfig.getSignatureForBlocks().encodingOf(nodeKeys.getPublic());
				var nodePublicKeyBase58 = Base58.encode(nodePublicKeyBytes);
				var fileNameNodeKeys = Paths.get(nodePublicKeyBase58 + ".pem");
				entropyForNode.dump(fileNameNodeKeys);
				System.out.println("Keys of the Mokamint node " + nodeNum + " dumped in file " + fileNameNodeKeys);

				var entropyForPlot = Entropies.random();
				KeyPair plotKeys = entropyForPlot.keys("", mokamintConfig.getSignatureForDeadlines());
				byte[] plotPublicKeyBytes = mokamintConfig.getSignatureForDeadlines().encodingOf(plotKeys.getPublic());
				var plotPublicKeyBase58 = Base58.encode(plotPublicKeyBytes);
				var fileNamePlotKeys = Paths.get(plotPublicKeyBase58 + ".pem");
				entropyForPlot.dump(fileNamePlotKeys);
				System.out.println("Keys of the miner of the Mokamint node " + nodeNum + " dumped in file " + fileNamePlotKeys);

				var prolog = Prologs.of(mokamintConfig.getChainId(), mokamintConfig.getSignatureForBlocks(), nodeKeys.getPublic(), mokamintConfig.getSignatureForDeadlines(), plotKeys.getPublic(), new byte[0]);

				System.out.println("Creating plot " + nodeNum + " of " + PLOT_LENGTH + " nonces");
				var plot = Plots.create(hotmokaChainPath.resolve("test.plot"), prolog, 1000, PLOT_LENGTH, mokamintConfig.getHashingForDeadlines(), __ -> {});
				plots.add(plot);

				var miner = LocalMiners.of(new PlotAndKeyPair[] { PlotAndKeyPairs.of(plot, plotKeys) });
				miners.add(miner);

				MokamintNode node = MokamintNodes.init(config, mokamintConfig, nodeKeys, nodeNum == 1); // we create a brand new genesis block, but only in node 1
				nodes.add(node);

				NodeServices.of(node, 8000 + nodeNum);
				System.out.println("Hotmoka node " + nodeNum + " published at ws://localhost:" + (8000 + nodeNum));
				int nodeNumCopy = nodeNum;
				node.getMokamintNode().add(miner).orElseThrow(() -> new NodeException("Could not add the miner to test node " + nodeNumCopy));

				// we open a web service to the underlying Mokamint node; this is not necessary,
				// but it allows developers to query the node during the execution of the tests
				var uri = URI.create("ws://localhost:" + (8029 + nodeNum));
				PublicNodeServices.open(node.getMokamintNode(), 8029 + nodeNum, 1800000, 1000, Optional.of(uri));
				System.out.println("Underlying Mokamint node " + nodeNum + " published at " + uri);

				if (nodeNum == 1) {
					firstNode = node;
					firstUri = uri;
					System.out.println("Initializing Hotmoka node " + nodeNum);
					initializeNodeIfNeeded(node);
				}
				else {
					try {
						if (firstNode.getMokamintNode().add(Peers.of(uri)).isPresent())
							System.out.println("Added " + uri + " as a peer of " + firstUri);
						else
							System.out.println("Could not add " + uri + " as a peer of " + firstUri);
					}
					catch (PeerRejectedException e) {
						System.out.println("Could not add " + uri + " as a peer of " + firstUri + ": " + e.getMessage());
					}
				}
			}

			return nodes.get(0);
		}
		catch (IOException | NoSuchAlgorithmException | io.mokamint.node.api.NodeException | DeploymentException | InvalidKeyException | SignatureException e) {
			throw new NodeException(e);
		}
	}
	
	@SuppressWarnings("unused")
	private static Node mkDiskNode() throws NodeException, InterruptedException {
		try {
			consensus = fillConsensusConfig(ConsensusConfigBuilders.defaults()).build();

			var config = DiskNodeConfigBuilders.defaults()
					.setDir(Files.createTempDirectory("hotmoka-disk-chain-"))
					.setMaxGasPerViewTransaction(_10_000_000)
					.setMaxPollingAttempts(100) // we fix these two so that we know the timeout in case of problems
					.setPollingDelay(10)
					.build();

			Node node = DiskNodes.init(config);
			nodes.add(node);
			return node;
		}
		catch (IOException | NoSuchAlgorithmException e) {
			throw new NodeException(e);
		}
	}

	@SuppressWarnings("unused")
	private static Node mkRemoteNode(Node exposed) throws IOException, DeploymentException {
		NodeServices.of(exposed, 8000); // it will be closed when exposed will be closed
		return RemoteNodes.of(URI.create("ws://localhost:8000"), 100_000);
	}

	@SuppressWarnings("unused")
	private static Node mkRemoteNode(String uri) throws IOException, DeploymentException {
		return RemoteNodes.of(URI.create(uri), 100_000);
	}

	private static <B extends ConsensusConfigBuilder<?,B>> B fillConsensusConfig(ConsensusConfigBuilder<?,B> builder) throws NodeException {
		try {
			return builder.setSignatureForRequests(SignatureAlgorithms.ed25519det()) // good for testing
					.allowUnsignedFaucet(true) // good for testing
					.ignoreGasPrice(true) // good for testing
					.setInitialSupply(Coin.level7(10000000)) // enough for all tests
					.setFinalSupply(Coin.level7(10000000).multiply(BigInteger.TWO))
					.setInitialRedSupply(Coin.level7(10000000)) // enough for all tests
					.setPublicKeyOfGamete(consensus.getPublicKeyOfGamete());
		}
		catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new NodeException(e);
		}
	}

	protected final void setAccounts(BigInteger... coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		nodeWithAccountsView = AccountsNodes.of(node, localGamete, privateKeyOfLocalGamete, coins);
	}

	protected final void setAccounts(String containerClassName, TransactionReference classpath, BigInteger... coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException, NodeException, NoSuchElementException, TimeoutException, InterruptedException, UnknownReferenceException {
		nodeWithAccountsView = AccountsNodes.of(node, localGamete, privateKeyOfLocalGamete, containerClassName, classpath, coins);
	}

	protected final void setAccounts(Stream<BigInteger> coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		setAccounts(coins.toArray(BigInteger[]::new));
	}

	protected final static AccountsNode mkAccounts(Stream<BigInteger> coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		return AccountsNodes.of(node, localGamete, privateKeyOfLocalGamete, coins.toArray(BigInteger[]::new));
	}

	protected final void setAccounts(String containerClassName, TransactionReference classpath, Stream<BigInteger> coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException, NodeException, NoSuchElementException, TimeoutException, InterruptedException, UnknownReferenceException {
		setAccounts(containerClassName, classpath, coins.toArray(BigInteger[]::new));
	}

	protected final void setGreenRedAccounts(BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchElementException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		nodeWithAccountsView = AccountsNodes.ofGreenRed(node, localGamete, privateKeyOfLocalGamete, coins);
	}

	protected static void setJar(String jar) throws TransactionRejectedException, TransactionException, IOException, NodeException, TimeoutException, InterruptedException {
		try {
			HotmokaTest.jar = JarsNodes.of(node, localGamete, privateKeyOfLocalGamete, pathOfExample(jar)).jar(0);
		}
		catch (NoSuchElementException e) {
			throw new NodeException(e); // we installed exactly one jar
		}
		catch (UnknownReferenceException e) {
			throw new NodeException(e); // the local gamete exists! We created it
		}
		catch (InvalidKeyException | SignatureException e) {
			throw new NodeException(e); // we set a correct key for the local gamete!
		}
	}

	protected final TransactionReference takamakaCode() {
		return takamakaCode;
	}

	protected final StorageReference manifest() {
		return manifest;
	}

	protected final static TransactionReference jar() {
		return jar;
	}

	protected final static String chainId() {
		return chainId;
	}

	protected final StorageReference account(int i) throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return nodeWithAccountsView.account(i);
	}

	protected final Stream<StorageReference> accounts() throws NodeException, TimeoutException, InterruptedException {
		return nodeWithAccountsView.accounts();
	}

	protected final StorageReference containerOfAccounts() throws NodeException, TimeoutException, InterruptedException {
		return nodeWithAccountsView.container();
	}

	protected final Stream<PrivateKey> privateKeys() throws NodeException, TimeoutException, InterruptedException {
		return nodeWithAccountsView.privateKeys();
	}

	protected final PrivateKey privateKey(int i) throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return nodeWithAccountsView.privateKey(i);
	}

	protected static SignatureAlgorithm signature() {
		return signature;
	}

	protected final TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, NodeException, TimeoutException, InterruptedException {
		return node.getRequest(reference);
	}

	protected final TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return node.getResponse(reference);
	}

	protected final TransactionReference addJarStoreInitialTransaction(byte[] jar, TransactionReference... dependencies) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return node.addJarStoreInitialTransaction(TransactionRequests.jarStoreInitial(jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final TransactionReference addJarStoreTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		return node.addJarStoreTransaction(TransactionRequests.jarStore(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageReference addConstructorCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		return node.addConstructorCallTransaction(TransactionRequests.constructorCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, constructor, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final void addInstanceVoidMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, VoidMethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue addInstanceNonVoidMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, NonVoidMethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		return node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals)).orElseThrow(() -> new NodeException(method + " did not return any value"));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue addStaticNonVoidMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, NonVoidMethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		return node.addStaticMethodCallTransaction(TransactionRequests.staticMethodCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, actuals)).orElseThrow(() -> new NodeException(method + " did not return any value"));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final void addStaticVoidMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, VoidMethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		node.addStaticMethodCallTransaction(TransactionRequests.staticMethodCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runInstanceNonVoidMethodCallTransaction(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, NonVoidMethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(caller, gasLimit, classpath, method, receiver, actuals)).orElseThrow(() -> new NodeException(method + " did not return any value"));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final void runInstanceVoidMethodCallTransaction(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, VoidMethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(caller, gasLimit, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runStaticNonVoidMethodCallTransaction(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, NonVoidMethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return node.runStaticMethodCallTransaction(TransactionRequests.staticViewMethodCall(caller, gasLimit, classpath, method, actuals)).orElseThrow(() -> new NodeException(method + " did not return any value"));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final void runStaticVoidMethodCallTransaction(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, VoidMethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		node.runStaticMethodCallTransaction(TransactionRequests.staticViewMethodCall(caller, gasLimit, classpath, method, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final JarFuture postJarStoreTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, InterruptedException, TimeoutException {
		return node.postJarStoreTransaction(TransactionRequests.jarStore(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final MethodFuture postInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, InterruptedException, TimeoutException {
		return node.postInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final ConstructorFuture postConstructorCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, InterruptedException, TimeoutException {
		return node.postConstructorCallTransaction(TransactionRequests.constructorCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, constructor, actuals));
	}

	protected static byte[] bytesOf(String fileName) throws IOException {
		return Files.readAllBytes(pathOfExample(fileName));
	}

	private static Path pathOfExample(String fileName) {
		return Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + '-' + fileName);
	}

	protected static void throwsTransactionExceptionWithCause(Class<? extends Throwable> expected, TestBody what) {
		var e = assertThrows(TransactionException.class, what::run);
		assertTrue(e.getMessage().startsWith(expected.getName()),
			() -> "wrong cause: expected " + expected.getName() + " but got " + e.getMessage());
	}

	protected static void throwsTransactionExceptionWithCauseAndMessageContaining(Class<? extends Throwable> expected, String subMessage, TestBody what) {
		throwsTransactionExceptionWithCauseAndMessageContaining(expected.getName(), subMessage, what);
	}

	protected static void throwsTransactionExceptionWithCauseAndMessageContaining(String prefix, String subMessage, TestBody what) {
		var e = assertThrows(TransactionException.class, what::run);
		assertTrue(e.getMessage().startsWith(prefix),
			() -> "wrong cause: expected " + prefix + " but got " + e.getMessage());
		assertTrue(e.getMessage().contains(subMessage),
			() -> "wrong message: it does not contain " + subMessage);
	}

	protected static void throwsTransactionExceptionWithCause(String expected, TestBody what) {
		var e = assertThrows(TransactionException.class, what::run);
		assertTrue(e.getMessage().startsWith(expected),
			() -> "wrong cause: expected " + expected + " but got " + e.getMessage());
	}

	protected static void throwsTransactionRejectedWithCause(String expected, TestBody what) {
		var e = assertThrows(TransactionRejectedException.class, what::run);
		assertTrue(e.getMessage().startsWith(expected),
			() -> "wrong cause: expected " + expected + " but got " + e.getMessage());
	}

	protected static void throwsVerificationException(TestBody what) {
		throwsTransactionExceptionWithCause(VerificationException.class, what);
	}

	protected static void throwsVerificationExceptionWithMessageContaining(String subMessage, TestBody what) {
		throwsTransactionExceptionWithCauseAndMessageContaining(VerificationException.class, subMessage, what);
	}

	/**
	 * Gets the nonce of the given account. It calls the {@code Account.nonce()} method.
	 * 
	 * @param account the account
	 * @return the nonce
	 */
	protected final BigInteger getNonceOf(StorageReference account) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		try {
			BigInteger nonce = nonces.get(account);
			// if there is more than one node or only a remote node, we need to ask the node since there might be history changes
			if (nonce != null && nodes.size() == 1)
				nonce = nonce.add(BigInteger.ONE);
			else
				// we ask the account: 100,000 units of gas should be enough to run the method
				nonce = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(account, _100_000, node.getClassTag(account).getJar(), MethodSignatures.NONCE, account))
					.orElseThrow(() -> new NodeException(MethodSignatures.NONCE + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.NONCE, NodeException::new);

			nonces.put(account, nonce);
			return nonce;
		}
		catch (CodeExecutionException | TransactionException e) {
			throw new NodeException("Cannot compute the nonce of " + account);
		}
		catch (UnknownReferenceException e) {
			throw new TransactionRejectedException(e, consensus);
		}
	}
}