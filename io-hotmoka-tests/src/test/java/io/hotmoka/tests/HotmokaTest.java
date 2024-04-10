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
 * MODIFY AT LINE 187 TO SELECT THE NODE IMPLEMENTATION TO TEST.
 */

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.api.requests.SignedTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.signatures.MethodSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.AccountsNodes;
import io.hotmoka.helpers.Coin;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.JarsNodes;
import io.hotmoka.helpers.api.AccountsNode;
import io.hotmoka.node.SimpleConsensusConfigBuilders;
import io.hotmoka.node.SimpleValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.ConsensusConfig;
import io.hotmoka.node.api.ConsensusConfigBuilder;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.ValidatorsConsensusConfig;
import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.DiskNodes;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.remote.RemoteNodeConfigBuilders;
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.node.service.NodeServiceConfigBuilders;
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
	 * The public key chosen for the gamete of the testing node.
	 */
	private final static String publicKeyOfGamete;

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
	 * True if and only if the tests are run with the Tendermint node. This node is slower,
	 * so that some tests will be reduced in complexity accordingly.
	 */
	private static boolean isUsingTendermint;

	/**
	 * The private key of the gamete.
	 */
	protected static final PrivateKey privateKeyOfGamete;

	private final static Logger LOGGER = Logger.getLogger(HotmokaTest.class.getName());

	public interface TestBody {
		void run() throws Exception;
	}

	static {
		try {
	        // we use always the same entropy and password, so that the tests become deterministic (if they are not explicitly non-deterministic)
	        var entropy = Entropies.of(new byte[16]);
			var password = "";
			var localSignature = SignatureAlgorithms.ed25519det();
			var keys = entropy.keys(password, localSignature);
			publicKeyOfGamete = Base64.toBase64String(localSignature.encodingOf(keys.getPublic()));
			consensus = SimpleValidatorsConsensusConfigBuilders.defaults()
	    			.signRequestsWith(SignatureAlgorithms.ed25519det()) // good for testing
	    			.allowUnsignedFaucet(true) // good for testing
	    			.ignoreGasPrice(true) // good for testing
	    			.setInitialSupply(Coin.level7(10000000)) // enough for all tests
	    			.setInitialRedSupply(Coin.level7(10000000)) // enough for all tests
	    			.setPublicKeyOfGamete(publicKeyOfGamete)
	    			.build();
	        privateKeyOfGamete = keys.getPrivate();

	        // Change this to test with different node implementations
	        Node wrapped;
	        node = wrapped = mkDiskBlockchain();
	        //node = wrapped = mkTendermintBlockchain();
	        //node = mkRemoteNode(wrapped = mkDiskBlockchain());
	        //node = mkRemoteNode(wrapped = mkTendermintBlockchain());
	        //node = wrapped = mkRemoteNode("ec2-54-194-239-91.eu-west-1.compute.amazonaws.com:8080");
	        //node = wrapped = mkRemoteNode("localhost:8080");

	        signature = SignatureAlgorithms.of(node.getConsensusConfig());
	        initializeNodeIfNeeded(wrapped);
	        Signer<SignedTransactionRequest<?>> signerOfGamete = signature.getSigner(privateKeyOfGamete, SignedTransactionRequest::toByteArrayWithoutSignature);

	        StorageReference manifest = node.getManifest();
	        var takamakaCode = node.getTakamakaCode();
	        var gamete = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
	    		(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest));

			chainId = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))).getValue();

			BigInteger nonce = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(gamete, _100_000, takamakaCode, MethodSignatures.NONCE, gamete))).getValue();

			BigInteger aLot = Coin.level6(1000000000);

			// we set the thresholds for the faucets of the gamete
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
			CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NodeException, TimeoutException, InterruptedException {

		try {
			node.getManifest();
		}
		catch (NoSuchElementException e) {
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
	private static Node mkTendermintBlockchain() throws IOException, NoSuchAlgorithmException {
		var consensus = fillConsensusConfig(SimpleValidatorsConsensusConfigBuilders.defaults()).build();
		HotmokaTest.consensus = consensus;

		isUsingTendermint = true;

		var config = TendermintNodeConfigBuilders.defaults()
			.setTendermintConfigurationToClone(Paths.get("tendermint_config"))
			.setMaxGasPerViewTransaction(_10_000_000)
			.build();

		return TendermintNodes.init(config, consensus);
	}

	private static <B extends ConsensusConfigBuilder<?,B>> B fillConsensusConfig(B builder) throws NoSuchAlgorithmException {
		return builder.signRequestsWith(SignatureAlgorithms.ed25519det()) // good for testing
			.allowUnsignedFaucet(true) // good for testing
			.ignoreGasPrice(true) // good for testing
			.setInitialSupply(Coin.level7(10000000)) // enough for all tests
			.setInitialRedSupply(Coin.level7(10000000)) // enough for all tests
			.setPublicKeyOfGamete(publicKeyOfGamete);
	}

	@SuppressWarnings("unused")
	private static Node mkDiskBlockchain() throws NoSuchAlgorithmException {
		var consensus = fillConsensusConfig(SimpleConsensusConfigBuilders.defaults()).build();
		HotmokaTest.consensus = consensus;

		var config = DiskNodeConfigBuilders.defaults()
			.setMaxGasPerViewTransaction(_10_000_000)
			.build();

		return DiskNodes.init(config, consensus);
	}

	@SuppressWarnings("unused")
	private static Node mkRemoteNode(Node exposed) throws IOException, DeploymentException {
		// we use port 8080, so that it does not interfere with the other service opened at port 8081 by the network tests
		var serviceConfig = NodeServiceConfigBuilders.defaults()
			.setPort(8080)
			.build();

		NodeServices.of(serviceConfig, exposed);

		var remoteNodeConfig = RemoteNodeConfigBuilders.defaults()
			// comment for using http
			//.usesWebSockets(true)
			.setURL("localhost:8080")
			.build();

		return RemoteNodes.of(remoteNodeConfig);
	}

	@SuppressWarnings("unused")
	private static Node mkRemoteNode(String url) throws IOException, DeploymentException {
		var remoteNodeConfig = RemoteNodeConfigBuilders.defaults()
			//.setWebSockets(true)
			.setURL(url).build();
		return RemoteNodes.of(remoteNodeConfig);
	}

	protected final void setAccounts(BigInteger... coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchElementException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException {
		nodeWithAccountsView = AccountsNodes.of(node, localGamete, privateKeyOfLocalGamete, coins);
	}

	protected final void setAccounts(String containerClassName, TransactionReference classpath, BigInteger... coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException, NodeException, NoSuchElementException, TimeoutException, InterruptedException {
		nodeWithAccountsView = AccountsNodes.of(node, localGamete, privateKeyOfLocalGamete, containerClassName, classpath, coins);
	}

	protected final void setAccounts(Stream<BigInteger> coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchElementException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException {
		setAccounts(coins.toArray(BigInteger[]::new));
	}

	protected final static AccountsNode mkAccounts(Stream<BigInteger> coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchElementException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException {
		return AccountsNodes.of(node, localGamete, privateKeyOfLocalGamete, coins.toArray(BigInteger[]::new));
	}

	protected final void setAccounts(String containerClassName, TransactionReference classpath, Stream<BigInteger> coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException, NodeException, NoSuchElementException, TimeoutException, InterruptedException {
		setAccounts(containerClassName, classpath, coins.toArray(BigInteger[]::new));
	}

	protected final void setGreenRedAccounts(BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchElementException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException {
		nodeWithAccountsView = AccountsNodes.ofGreenRed(node, localGamete, privateKeyOfLocalGamete, coins);
	}

	protected static void setJar(String jar) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, IOException, NoSuchElementException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException {
		HotmokaTest.jar = JarsNodes.of(node, localGamete, privateKeyOfLocalGamete, pathOfExample(jar)).jar(0);
	}

	protected final TransactionReference takamakaCode() throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return node.getTakamakaCode();
	}

	protected static TransactionReference jar() {
		return jar;
	}

	protected final StorageReference account(int i) {
		return nodeWithAccountsView.account(i);
	}

	protected final Stream<StorageReference> accounts() {
		return nodeWithAccountsView.accounts();
	}

	protected final StorageReference containerOfAccounts() {
		return nodeWithAccountsView.container();
	}

	protected final Stream<PrivateKey> privateKeys() {
		return nodeWithAccountsView.privateKeys();
	}

	protected final PrivateKey privateKey(int i) {
		return nodeWithAccountsView.privateKey(i);
	}

	protected static SignatureAlgorithm signature() {
		return signature;
	}

	protected static boolean isUsingTendermint() {
		return isUsingTendermint;
	}

	protected final TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return node.getRequest(reference);
	}

	protected final TransactionResponse getResponse(TransactionReference reference) throws NoSuchElementException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return node.getResponse(reference);
	}

	protected final TransactionReference addJarStoreInitialTransaction(byte[] jar, TransactionReference... dependencies) throws TransactionRejectedException {
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
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws NodeException 
	 */
	protected final StorageValue addInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		return node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws NodeException 
	 */
	protected final StorageValue addStaticMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		return node.addStaticMethodCallTransaction(TransactionRequests.staticMethodCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runInstanceMethodCallTransaction(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(caller, gasLimit, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runStaticMethodCallTransaction(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return node.runStaticMethodCallTransaction(TransactionRequests.staticViewMethodCall(caller, gasLimit, classpath, method, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final JarSupplier postJarStoreTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, InterruptedException, TimeoutException {
		return node.postJarStoreTransaction(TransactionRequests.jarStore(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeSupplier<StorageValue> postInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, InterruptedException, TimeoutException {
		return node.postInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeSupplier<StorageReference> postConstructorCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, InterruptedException, TimeoutException {
		return node.postConstructorCallTransaction(TransactionRequests.constructorCall(signature.getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, constructor, actuals));
	}

	protected static byte[] bytesOf(String fileName) throws IOException {
		return Files.readAllBytes(pathOfExample(fileName));
	}

	protected static Path pathOfExample(String fileName) {
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

	protected static void throwsTransactionRejectedWithCause(Class<? extends Throwable> expected, TestBody what) {
		var e = assertThrows(TransactionRejectedException.class, what::run);
		assertTrue(e.getMessage().startsWith(expected.getName()),
			() -> "wrong cause: expected " + expected.getName() + " but got " + e.getMessage());
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

	protected static void throwsTransactionException(TestBody what) {
		assertThrows(TransactionException.class, what::run);
	}

	protected static void throwsTransactionRejectedException(TestBody what) {
		assertThrows(TransactionRejectedException.class, what::run);
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
	 * @throws TransactionRejectedException if the nonce cannot be found
	 */
	protected final BigInteger getNonceOf(StorageReference account) throws TransactionRejectedException {
		try {
			BigInteger nonce = nonces.get(account);
			if (nonce != null)
				nonce = nonce.add(BigInteger.ONE);
			else
				// we ask the account: 100,000 units of gas should be enough to run the method
				nonce = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(account, _100_000, node.getClassTag(account).getJar(), MethodSignatures.NONCE, account))).getValue();

			nonces.put(account, nonce);
			return nonce;
		}
		catch (Exception e) {
			LOGGER.log(Level.SEVERE, "failed computing nonce", e);
			throw new TransactionRejectedException("Cannot compute the nonce of " + account);
		}
	}

	/**
	 * Gets the (green) balance of the given account. It calls the {@code AccountWithAccessibleBalance.getBalance()} method.
	 * 
	 * @param account the account
	 * @return the balance
	 * @throws TransactionRejectedException if the balance cannot be found
	 */
	protected static BigInteger getBalanceOf(StorageReference account) throws TransactionRejectedException {
		try {
			// we ask the account: 10,000 units of gas should be enough to run the method
			var classTag = node.getClassTag(account);
			return ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(account, _100_000, classTag.getJar(), MethodSignatures.BALANCE, account))).getValue();
		}
		catch (Exception e) {
			LOGGER.log(Level.SEVERE, "failed computing the balance", e);
			throw new TransactionRejectedException("Cannot compute the balance of " + account);
		}
	}
}