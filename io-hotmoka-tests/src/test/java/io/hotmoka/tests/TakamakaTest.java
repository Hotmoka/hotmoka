package io.hotmoka.tests;

/**
 * MODIFY AT LINE 187 TO SELECT THE NODE IMPLEMENTATION TO TEST.
 */
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.Coin;
import io.hotmoka.beans.SignatureAlgorithm;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.local.Config;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.Node.CodeSupplier;
import io.hotmoka.nodes.Node.JarSupplier;
import io.hotmoka.remote.RemoteNode;
import io.hotmoka.remote.RemoteNodeConfig;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import io.hotmoka.takamaka.DeltaGroupExecutionResult;
import io.hotmoka.takamaka.TakamakaBlockchain;
import io.hotmoka.takamaka.TakamakaBlockchainConfig;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.views.TendermintInitializedNode;
import io.hotmoka.views.InitializedNode;
import io.hotmoka.views.NodeWithAccounts;
import io.hotmoka.views.NodeWithJars;
import io.takamaka.code.verification.VerificationException;

public abstract class TakamakaTest {

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
	 * each test will decorate it into {@linkplain #nodeWithAccountsView},
	 * with the addition of the jar and accounts that the test needs.
	 */
	protected final static Node node;

	/**
	 * The configuration of the node, if it is not a remote node.
	 */
	protected static Config nodeConfig;

	/**
	 * The consensus parameters of the node.
	 */
	protected static ConsensusParams consensus;

	/**
	 * The private key of the gamete.
	 */
	private static PrivateKey privateKeyOfGamete;

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
	private final static SignatureAlgorithm<SignedTransactionRequest> signature;

	/**
	 * The jar under test.
	 */
	private static TransactionReference jar;

	/**
	 * The node under test. This is a view of {@linkplain #node},
	 * with the addition of some initial accounts, recreated before each test.
	 */
	private NodeWithAccounts nodeWithAccountsView;

	/**
	 * The nonce of each externally owned account used in the test.
	 */
	private final ConcurrentMap<StorageReference, BigInteger> nonces = new ConcurrentHashMap<>();

	/**
	 * The chain identifier of the node used for the tests.
	 */
	protected final static String chainId;

	/**
	 * Non-null if the node is based on Tendermint, so that a specific initialization can be run.
	 */
	protected static TendermintBlockchain tendermintBlockchain;

	/**
	 * Non-null if the node is based on AILIA's Takamaka blockchain, so that a specific initialization can be run.
	 */
	protected static TakamakaBlockchain takamakaBlockchain;

	/**
	 * The version of the Takamaka project, as stated in the pom file.
	 */
	private final static String takamakaVersion;

	/**
	 * The version of the Hotmoka project, as stated in the pom file.
	 */
	private final static String hotmokaVersion;

	private final static Logger logger = LoggerFactory.getLogger(TakamakaTest.class);

	@BeforeEach
	void logTestName(TestInfo testInfo) {
		logger.info("**** Starting test " + testInfo.getTestClass().get().getSimpleName() + '.' + testInfo.getTestMethod().get().getName() + ": " + testInfo.getDisplayName());
	}

	public interface TestBody {
		void run() throws Exception;
	}

	static {
		try {
			// we access the project.version property from the pom.xml file of the parent project
			MavenXpp3Reader reader = new MavenXpp3Reader();
	        Model model = reader.read(new FileReader("../pom.xml"));
	        takamakaVersion = (String) model.getProperties().get("takamaka.version");
	        hotmokaVersion = (String) model.getProperties().get("hotmoka.version");
	        tendermintBlockchain = null; // Tendermint would reassign

	        // Change this to test with different node implementations
	        node = mkMemoryBlockchain();
	        //node = mkTendermintBlockchain();
	    	//node = mkTakamakaBlockchainExecuteOneByOne();
	        //node = mkTakamakaBlockchainExecuteAtEachTimeslot();
	        //node = mkRemoteNode(mkMemoryBlockchain());
	        //node = mkRemoteNode(mkTendermintBlockchain());
	        //node = mkRemoteNode(mkTakamakaBlockchainExecuteOneByOne());
	        //node = mkRemoteNode(mkTakamakaBlockchainExecuteAtEachTimeslot());
	        //node = mkRemoteNode("ec2-54-194-239-91.eu-west-1.compute.amazonaws.com:8080");
	        //node = mkRemoteNode("localhost:8080");

	        signature = io.hotmoka.crypto.SignatureAlgorithm.mk(node.getSignatureAlgorithmForRequests(), SignedTransactionRequest::toByteArrayWithoutSignature);
	        // dump the key if you want to generate the signature file for a new signature algorithm
	        //dumpKeys(signature.getKeyPair());
	        initializeNodeIfNeeded();

	        StorageReference manifest = node.getManifest();
	        TransactionReference takamakaCode = node.getTakamakaCode();
	        StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
	    		(manifest, _100_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

			chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;

			BigInteger nonce = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(gamete, _100_000, takamakaCode, CodeSignature.NONCE, gamete))).value;

			BigInteger aLot = Coin.level6(1000000000);

			// we set the thresholds for the faucets of the gamete
			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(Signer.with(signature, privateKeyOfGamete), gamete, nonce, chainId, _100_000, BigInteger.ONE, takamakaCode,
				new VoidMethodSignature(ClassType.GAMETE, "setMaxFaucet", ClassType.BIG_INTEGER, ClassType.BIG_INTEGER), gamete,
				new BigIntegerValue(aLot), new BigIntegerValue(aLot)));

	        NodeWithAccounts local = NodeWithAccounts.ofGreenRed(node, gamete, privateKeyOfGamete, aLot, aLot);
	        localGamete = local.account(0);
	        privateKeyOfLocalGamete = local.privateKey(0);
		}
		catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Dumps into a file the key pair used for the gamete in the tests.
	 */
	@SuppressWarnings("unused")
	private static void dumpKeys(KeyPair keys) throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("gamete.keys"))) {
            oos.writeObject(keys);
            System.out.println("The keys of the gamete have been succesfully written into the file gamete.keys");
        }
	}

	private static KeyPair loadKeysOfGamete() throws ClassNotFoundException, IOException, NoSuchAlgorithmException {
		String fileWithKeys;
		String signatureName = signature.getClass().getName();
		// for the empty signature algorithm, the actual keys are irrelevant
		if (signatureName.endsWith("ED25519") || signatureName.endsWith("EMPTY") || signatureName.endsWith("ED25519DET"))
			fileWithKeys = "gameteED25519.keys";
		else if (signatureName.endsWith("SHA256DSA"))
			fileWithKeys = "gameteSHA256DSA.keys";
		else if (signatureName.endsWith("QTESLA1") || signatureName.endsWith("QTESLA3"))
			fileWithKeys = "gameteQTesla.keys";
		else
			throw new NoSuchAlgorithmException("I have no keys for signing algorithm " + signatureName);

		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileWithKeys))) {
			return (KeyPair) ois.readObject();
		}
	}

	private static void initializeNodeIfNeeded() throws TransactionRejectedException, TransactionException,
			CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException {

		KeyPair keysOfGamete = loadKeysOfGamete();

		try {
			node.getManifest();
		}
		catch (NoSuchElementException e) {
			// if the original node has no manifest yet, it means that it is not initialized and we initialize it

			// enough for all tests
			BigInteger aLot = Coin.level7(10000000);
			Path takamakaCode = Paths.get("../modules/explicit/io-takamaka-code-" + takamakaVersion + ".jar");

			if (tendermintBlockchain != null)
				TendermintInitializedNode.of(tendermintBlockchain, consensus, keysOfGamete, takamakaCode, aLot, aLot);
			else
				InitializedNode.of(node, consensus, keysOfGamete, takamakaCode, aLot, aLot);
		}

		privateKeyOfGamete = keysOfGamete.getPrivate();
	}

	@SuppressWarnings("unused")
	private static Node mkTendermintBlockchain() throws NoSuchAlgorithmException {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder()
			.setTendermintConfigurationToClone(Paths.get("tendermint_config"))
			.setMaxGasPerViewTransaction(_10_000_000)
			.build();
		nodeConfig = config;
		consensus = new ConsensusParams.Builder()
			.signRequestsWith("ed25519det") // good for testing
			.allowUnsignedFaucet(true) // good for testing
			.ignoreGasPrice(true) // good for testing
			.build();

		TendermintBlockchain result = TendermintBlockchain.init(config, consensus);
		tendermintBlockchain = result;
		return result;
	}

	@SuppressWarnings("unused")
	private static Node mkMemoryBlockchain() throws NoSuchAlgorithmException {
		MemoryBlockchainConfig config = new MemoryBlockchainConfig.Builder()
			.setMaxGasPerViewTransaction(_10_000_000)
			.build();

		nodeConfig = config;

		// specify the signing algorithm, if you need; otherwise ED25519 will be used by default
		consensus = new ConsensusParams.Builder()
			.signRequestsWith("ed25519det") // good for testing
			.allowUnsignedFaucet(true) // good for testing
			// .signRequestsWith("qtesla1").build();
			// .signRequestsWith("qtesla3").build();
			// .signRequestsWith("sha256dsa").build();
			.setChainId("test")
			.ignoreGasPrice(true) // good for testing
			.build();

		return MemoryBlockchain.init(config, consensus);
	}

	@SuppressWarnings("unused")
	private static Node mkTakamakaBlockchainExecuteOneByOne() throws NoSuchAlgorithmException {
		TakamakaBlockchainConfig config = new TakamakaBlockchainConfig.Builder().setMaxGasPerViewTransaction(_10_000_000).build();
		nodeConfig = config;
		consensus = new ConsensusParams.Builder()
			.setChainId("test")
			.signRequestsWith("ed25519det") // good for testing
			.ignoreGasPrice(true) // good for testing
			.allowSelfCharged(true) // only for this kind of node
			.build();
		return takamakaBlockchain = TakamakaBlockchain.init(config, consensus, TakamakaBlockchainOneByOne::postTransactionTakamakaBlockchainRequestsOneByOne);
	}

	// this code must stay in its own class, or otherwise the static initialization of TakamakaTest goes into an infinite loop!
	private static class TakamakaBlockchainOneByOne {
		private static byte[] hash; // used for the simulation of the Takamaka blockchain only

		/**
		 * This simulates the implementation of postTransaction() in such a way to put
		 * each request in a distinct delta group. By making this method synchronized,
		 * we avoid that two delta groups get executed in parallel.
		 * 
		 * @param request the request
		 */
		private static synchronized void postTransactionTakamakaBlockchainRequestsOneByOne(TransactionRequest<?> request) {
			DeltaGroupExecutionResult result = takamakaBlockchain.execute(hash, System.currentTimeMillis(), Stream.of(request), Stream.of(BigInteger.ZERO), "id");
			hash = result.getHash();
			takamakaBlockchain.checkOut(hash);
		}
	}

	// this code must stay in its own class, or otherwise the static initialization of TakamakaTest goes
	// into an infinite loop!
	private static class TakamakaBlockchainAtEachTimeslot {
		private final static List<TransactionRequest<?>> mempool = new ArrayList<>();

		/**
		 * This simulates the implementation of postTransaction() in such a way to put
		 * each request in a distinct delta group.
		 * 
		 * @param request the request
		 */
		private static void postTransactionTakamakaBlockchainRequestsOneByOne(TransactionRequest<?> request) {
			synchronized (mempool) {
				mempool.add(request);
			}
		}
	}

	@SuppressWarnings("unused")
	private static Node mkTakamakaBlockchainExecuteAtEachTimeslot() throws NoSuchAlgorithmException {
		TakamakaBlockchainConfig config = new TakamakaBlockchainConfig.Builder().setMaxGasPerViewTransaction(_10_000_000).build();
		nodeConfig = config;
		consensus = new ConsensusParams.Builder()
			.setChainId("test")
			.ignoreGasPrice(true) // good for testing
			.allowSelfCharged(true) // only for this kind of node
			.allowUnsignedFaucet(true) // good for testing
			.build();

		List<TransactionRequest<?>> mempool = TakamakaBlockchainAtEachTimeslot.mempool;

		// we provide an implementation of postTransaction() that just adds the request in the mempool
		takamakaBlockchain = TakamakaBlockchain.init(config, consensus, TakamakaBlockchainAtEachTimeslot::postTransactionTakamakaBlockchainRequestsOneByOne);
		TakamakaBlockchain local = takamakaBlockchain;

		// we start a scheduler that checks the mempool every time-slot to see if there are requests to execute
		Thread scheduler = new Thread(() -> {
			byte[] hash = null;

			while (true) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {}

				// we check if a previous execute() is still running,
				// since we cannot run two execute() at the same time
				if (local.getCurrentExecutionId().isEmpty()) {
					Stream<TransactionRequest<?>> requests;
					int size;

					synchronized (mempool) {
						int mempoolSize = mempool.size();
						if (mempoolSize == 0)
							// it is possible, but useless, to start an empty execute()
							continue;

						// the clone of the mempool is needed or otherwise a concurrent modification exception might occur later
						requests = new ArrayList<>(mempool).stream();
						size = mempool.size();
						mempool.clear();
					}

					DeltaGroupExecutionResult result = local.execute(hash, System.currentTimeMillis(), requests, Stream.generate(() -> BigInteger.ZERO).limit(size), "id");
					hash = result.getHash();
					local.checkOut(hash);
				}
			}
		});

		scheduler.start();

		logger.info("scheduled mempool check every 100 milliseconds");
		return takamakaBlockchain;
	}

	@SuppressWarnings("unused")
	private static Node mkRemoteNode(Node exposed) {
		// we use port 8080, so that it does not interfere with the other service opened at port 8081 by the network tests
		NodeServiceConfig serviceConfig = new NodeServiceConfig.Builder()
			.setPort(8080)
			.setSpringBannerModeOn(false).build();

		NodeService.of(serviceConfig, exposed);

		RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder()
			//.setWebSockets(false).setURL("localhost:8080")
			// uncomment for using websockets
			//.setWebSockets(true)
			.setURL("localhost:8080")
			.build();

		return RemoteNode.of(remoteNodeConfig);
	}

	@SuppressWarnings("unused")
	private static Node mkRemoteNode(String url) {
		RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder()
			//.setWebSockets(true)
			.setURL(url).build();
		return RemoteNode.of(remoteNodeConfig);
	}

	protected final void setAccounts(BigInteger... coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException {
		nodeWithAccountsView = NodeWithAccounts.of(node, localGamete, privateKeyOfLocalGamete, coins);
	}

	protected final void setAccounts(String containerClassName, TransactionReference classpath, BigInteger... coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException {
		nodeWithAccountsView = NodeWithAccounts.of(node, localGamete, privateKeyOfLocalGamete, containerClassName, classpath, coins);
	}

	protected final void setAccounts(Stream<BigInteger> coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException {
		setAccounts(coins.toArray(BigInteger[]::new));
	}

	protected final void setAccounts(String containerClassName, TransactionReference classpath, Stream<BigInteger> coins) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException {
		setAccounts(containerClassName, classpath, coins.toArray(BigInteger[]::new));
	}

	protected final void setGreenRedAccounts(BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		nodeWithAccountsView = NodeWithAccounts.ofGreenRed(node, localGamete, privateKeyOfLocalGamete, coins);
	}

	protected static void setJar(String jar) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, IOException {
		TakamakaTest.jar = NodeWithJars.of(node, localGamete, privateKeyOfLocalGamete, pathOfExample(jar)).jar(0);
	}

	protected final TransactionReference takamakaCode() {
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

	protected final SignatureAlgorithm<SignedTransactionRequest> signature() {
		return signature;
	}

	protected final TransactionRequest<?> getRequest(TransactionReference reference) {
		return node.getRequest(reference);
	}

	protected final TransactionResponse getResponse(TransactionReference reference) throws NoSuchElementException, TransactionRejectedException {
		return node.getResponse(reference);
	}

	protected final TransactionReference addJarStoreInitialTransaction(byte[] jar, TransactionReference... dependencies) throws TransactionRejectedException {
		return node.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final TransactionReference addJarStoreTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return node.addJarStoreTransaction(new JarStoreTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageReference addConstructorCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return node.addConstructorCallTransaction(new ConstructorCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, constructor, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue addInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue addStaticMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return node.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runInstanceMethodCallTransaction(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException {
		return node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(caller, gasLimit, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runStaticMethodCallTransaction(StorageReference caller, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException {
		return node.runStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(caller, gasLimit, classpath, method, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final JarSupplier postJarStoreTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		return node.postJarStoreTransaction(new JarStoreTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeSupplier<StorageValue> postInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		return node.postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeSupplier<StorageReference> postConstructorCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		return node.postConstructorCallTransaction(new ConstructorCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller), chainId, gasLimit, gasPrice, classpath, constructor, actuals));
	}

	protected static byte[] bytesOf(String fileName) throws IOException {
		return Files.readAllBytes(pathOfExample(fileName));
	}

	protected static Path pathOfExample(String fileName) {
		return Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + hotmokaVersion + '-' + fileName);
	}

	protected static void throwsTransactionExceptionWithCause(Class<? extends Throwable> expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			if (e.getMessage().startsWith(expected.getName()))
				return;

			fail("wrong cause: expected " + expected.getName() + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionExceptionWithCauseAndMessageContaining(Class<? extends Throwable> expected, String subMessage, TestBody what) {
		throwsTransactionExceptionWithCauseAndMessageContaining(expected.getName(), subMessage, what);
	}

	protected static void throwsTransactionExceptionWithCauseAndMessageContaining(String expected, String subMessage, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			if (e.getMessage().startsWith(expected)) {
				if (e.getMessage().contains(subMessage))
					return;

				fail("wrong message: it does not contain " + subMessage);
			}

			fail("wrong cause: expected " + expected + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionRejectedWithCause(Class<? extends Throwable> expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionRejectedException e) {
			if (e.getMessage().startsWith(expected.getName()))
				return;

			fail("wrong cause: expected " + expected.getName() + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionRejectedException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionRejectedException.class.getName());
	}

	protected static void throwsTransactionExceptionWithCause(String expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			if (e.getMessage().startsWith(expected))
				return;

			fail("wrong cause: expected " + expected + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionRejectedWithCause(String expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionRejectedException e) {
			if (e.getMessage().startsWith(expected))
				return;

			fail("wrong cause: expected " + expected + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionRejectedException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionRejectedException.class.getName());
	}

	protected static void throwsTransactionException(TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			return;
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionRejectedException(TestBody what) {
		try {
			what.run();
		}
		catch (TransactionRejectedException e) {
			return;
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionRejectedException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionRejectedException.class.getName());
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
				// we ask the account: 10,000 units of gas should be enough to run the method
				nonce = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(account, _100_000, node.getClassTag(account).jar, CodeSignature.NONCE, account))).value;

			nonces.put(account, nonce);
			return nonce;
		}
		catch (Exception e) {
			logger.error("failed computing nonce", e);
			throw new TransactionRejectedException("cannot compute the nonce of " + account);
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
			ClassTag classTag = node.getClassTag(account);
			return ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(account, _100_000, classTag.jar, CodeSignature.BALANCE, account))).value;
		}
		catch (Exception e) {
			logger.error("failed computing the balance", e);
			throw new TransactionRejectedException("cannot compute the balance of " + account);
		}
	}
}
