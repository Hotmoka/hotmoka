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
 * MODIFY AT LINE 202 TO SELECT THE NODE IMPLEMENTATION TO TEST.
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
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;

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
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.tendermint.TendermintInitializedNodes;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import io.hotmoka.node.tendermint.api.TendermintNode;
import io.hotmoka.testing.AbstractLoggedTests;
import io.hotmoka.verification.VerificationException;
import io.takamaka.code.constants.Constants;
import jakarta.websocket.DeploymentException;

public abstract class HotmokaTest extends AbstractLoggedTests {
	protected static final BigInteger _50_000 = BigInteger.valueOf(50_000);
	protected static final BigInteger _100_000 = BigInteger.valueOf(100_000);
	protected static final BigInteger _500_000 = BigInteger.valueOf(500_000);
	protected static final BigInteger _1_000_000 = BigInteger.valueOf(1_000_000);
	protected static final BigInteger _10_000_000 = BigInteger.valueOf(10_000_000);
	protected static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);
	protected static final BigInteger _10_000_000_000 = BigInteger.valueOf(10_000_000_000L);

	/**
	 * The node that gets created before starting running the tests.
	 * This node will hence be created only once and
	 * each test will create the accounts and add the jars that it needs.
	 */
	protected final static Node node;

	/**
	 * The consensus parameters of the node.
	 */
	protected static ConsensusConfig<?,?> consensus;

	/**
	 * The private key of the account used at each run of the tests.
	 */
	private final static PrivateKey privateKeyOfLocalGamete;

	/**
	 * The account that can be used as gamete for each run of the tests.
	 */
	private final static StorageReference localGamete;

	/**
	 * The signature algorithm used for signing the requests.
	 */
	private final static SignatureAlgorithm signature;

	/**
	 * The reference to the manifest in the test node.
	 */
	private final static StorageReference manifest;

	/**
	 * The transaction that installed the Takamaka runtime in the test node.
	 */
	private final static TransactionReference takamakaCode;

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
	protected final static String chainId;

	/**
	 * The private key of the gamete.
	 */
	private static final PrivateKey privateKeyOfGamete;

	public interface TestBody {
		void run() throws Exception;
	}

	static {
		try {
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
	        node = wrapped = mkDiskBlockchain();
	        //node = wrapped = mkTendermintBlockchain();
	        //node = mkRemoteNode(wrapped = mkDiskBlockchain());
	        //node = mkRemoteNode(wrapped = mkTendermintBlockchain());
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
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
			throw new ExceptionInInitializerError(e);
		}
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
	private static Node mkTendermintBlockchain() throws NodeException, InterruptedException {
		try {
			var consensus = fillConsensusConfig(ValidatorsConsensusConfigBuilders.defaults()).build();
			HotmokaTest.consensus = consensus;

			var config = TendermintNodeConfigBuilders.defaults()
					.setDir(Files.createTempDirectory("hotmoka-chain"))
					.setTendermintConfigurationToClone(Paths.get("tendermint_config"))
					.setMaxGasPerViewTransaction(_10_000_000)
					.build();

			return TendermintNodes.init(config, consensus);
		}
		catch (IOException | NoSuchAlgorithmException e) {
			throw new NodeException(e);
		}
	}

	private static <B extends ConsensusConfigBuilder<?,B>> B fillConsensusConfig(ConsensusConfigBuilder<?,B> builder) throws NodeException {
		try {
			return builder.setSignatureForRequests(SignatureAlgorithms.ed25519det()) // good for testing
					.allowUnsignedFaucet(true) // good for testing
					.ignoreGasPrice(true) // good for testing
					.setInitialSupply(Coin.level7(10000000)) // enough for all tests
					.setInitialRedSupply(Coin.level7(10000000)) // enough for all tests
					.setPublicKeyOfGamete(consensus.getPublicKeyOfGamete());
		}
		catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new NodeException(e);
		}
	}

	@SuppressWarnings("unused")
	private static Node mkDiskBlockchain() throws NodeException, InterruptedException {
		try {
			var consensus = fillConsensusConfig(ConsensusConfigBuilders.defaults()).build();
			HotmokaTest.consensus = consensus;

			var config = DiskNodeConfigBuilders.defaults()
					.setDir(Files.createTempDirectory("hotmoka-chain"))
					.setMaxGasPerViewTransaction(_10_000_000)
					.setMaxPollingAttempts(100) // we fix these two so that we know the timeout in case of problems
					.setPollingDelay(10)
					.build();

			return DiskNodes.init(config, consensus);
		}
		catch (IOException | NoSuchAlgorithmException e) {
			throw new NodeException(e);
		}
	}

	@SuppressWarnings("unused")
	private static Node mkRemoteNode(Node exposed) throws IOException, DeploymentException {
		NodeServices.of(exposed, 8001); // it will close when exposed will be closed
		return RemoteNodes.of(URI.create("ws://localhost:8001"), 100_000);
	}

	@SuppressWarnings("unused")
	private static Node mkRemoteNode(String uri) throws IOException, DeploymentException {
		return RemoteNodes.of(URI.create(uri), 100_000);
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
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws NodeException 
	 */
	protected final StorageValue addStaticNonVoidMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, NonVoidMethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		return node.addStaticMethodCallTransaction(TransactionRequests.staticMethodCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, actuals)).orElseThrow(() -> new NodeException(method + " did not return any value"));
	}

	/**
	 * Takes care of computing the next nonce.
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws NodeException 
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
			if (nonce != null)
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