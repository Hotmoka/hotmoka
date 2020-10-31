package io.takamaka.code.tests;

/**
 * MODIFY AT LINE 167 TO SELECT THE NODE IMPLEMENTATION TO TEST.
 * MODIFY LINE 86 IF YOU ARE RUNNING AGAINST AN ALREADY INITIALIZED
 * NODE, WITH A GIVEN GAMETE.
 */
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest.Signer;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.network.NodeService;
import io.hotmoka.network.NodeServiceConfig;
import io.hotmoka.network.RemoteNode;
import io.hotmoka.network.RemoteNodeConfig;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.Node.CodeSupplier;
import io.hotmoka.nodes.Node.JarSupplier;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.nodes.views.NodeWithAccounts;
import io.hotmoka.nodes.views.NodeWithJars;
import io.hotmoka.takamaka.DeltaGroupExecutionResult;
import io.hotmoka.takamaka.TakamakaBlockchain;
import io.hotmoka.takamaka.TakamakaBlockchainConfig;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.Config;
import io.takamaka.code.verification.VerificationException;

public abstract class TakamakaTest {

	/**
	 * Change this if you are running the test against a node that
	 * has been already initialized with a given gamete.
	 */
	private final static StorageReference DEFAULT_GAMETE = new StorageReference
		(new LocalTransactionReference("d56369c8d1d7c7fe54599e3897126ae6fec3c05a1c3e3cc2a8aae00ea1a67c5c"), BigInteger.ZERO);

	/**
	 * The node that gets created before starting running the tests.
	 * This node will hence be created only once and
	 * each test will decorate it into {@linkplain #nodeWithAccountsView},
	 * with the addition of the jar and accounts that the test needs.
	 */
	protected final static Node originalView;

	/**
	 * The configuration of the node, if it is not a remote node.
	 */
	protected static Config originalConfig;

	/**
	 * The account that can be used as gamete, globally for all tests.
	 */
	private static StorageReference gamete;

	/**
	 * The private key of {@link #gamete}.
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
	private final static SignatureAlgorithm<NonInitialTransactionRequest<?>> signature;

	/**
	 * The node under test. This is a view of {@linkplain #originalView},
	 * with the addition of some jars for testing, recreated before each test.
	 */
	protected NodeWithJars nodeWithJarsView;

	/**
	 * The node under test. This is a view of {@linkplain #originalView},
	 * with the addition of some initial accounts, recreated before each test.
	 */
	protected NodeWithAccounts nodeWithAccountsView;

	/**
	 * The nonce of each externally owned account used in the test.
	 */
	private final ConcurrentMap<StorageReference, BigInteger> nonces = new ConcurrentHashMap<>();

	/**
	 * The chain identifier of the node used for the tests.
	 */
	protected final static String chainId;

	/**
	 * The version of the project, as stated in the pom file.
	 */
	private final static String version;

	private final static Logger logger = LoggerFactory.getLogger(TakamakaTest.class);

	@BeforeEach
	void logTestName(TestInfo testInfo) {
		logger.info("**** Starting test " + testInfo.getTestClass().get().getSimpleName() + '.' + testInfo.getTestMethod().get().getName() + ": " + testInfo.getDisplayName());
	}

	public interface TestBody {
		public void run() throws Exception;
	}

	static {
		try {
			// we access the project.version property from the pom.xml file of the parent project
			MavenXpp3Reader reader = new MavenXpp3Reader();
	        Model model = reader.read(new FileReader("../pom.xml"));
	        version = (String) model.getProperties().get("project.version");
	        chainId = TakamakaTest.class.getName();

	        // Change this to test with different node implementations
	    	originalView = mkMemoryBlockchain();
	        //originalView = mkTendermintBlockchain();
	        //originalView = mkTakamakaBlockchainExecuteOneByOne();
	        //originalView = mkTakamakaBlockchainExecuteAtEachTimeslot();
	        //originalView = mkRemoteNode(mkMemoryBlockchain());
	        //originalView = mRemoteNode(mkTendermintBlockchain());
	        //originalView = mkRemoteNode(mkTakamakaBlockchainExecuteOneByOne());
	        //originalView = mkRemoteNode(mkTakamakaBlockchainExecuteAtEachTimeslot());
	        //originalView = mkRemoteNode("ec2-54-194-239-91.eu-west-1.compute.amazonaws.com:8080");
	        //originalView = mkRemoteNode("localhost:8080");

	        signature = originalView.getSignatureAlgorithmForRequests();
	        // dump the key if you want to generate the signature file for a new signature algorithm
	        //dumpKeys(signature.getKeyPair());
	        initializeNodeIfNeeded();

	        // we create a node that will pay for the initialization of each test;
	        // this could be the gamete, but then there will be race conditions if the tests
	        // are run concurrently against the same node, by two machines;
	        // by using a local gamete, the risk of race condition is limited to this line,
	        // when we check the nonce of the (global) gamete and use it immediately after to
	        // create the local gamete
	        NodeWithAccounts local = NodeWithAccounts.ofRedGreen(originalView, gamete, privateKeyOfGamete, BigInteger.valueOf(999_999_999).pow(4), BigInteger.valueOf(999_999_999).pow(4));
	        localGamete = local.account(0);
	        privateKeyOfLocalGamete = local.privateKey(0);
		}
		catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Dumps into a file the key pair used for the gamete in the tests.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@SuppressWarnings("unused")
	private static void dumpKeys(KeyPair keys) throws FileNotFoundException, IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("gamete.keys"))) {
            oos.writeObject(keys);
            System.out.println("The keys of the gamete have been succesfully written into the file gamete.keys");
        }
	}

	private static KeyPair loadKeysOfGamete() throws ClassNotFoundException, IOException, NoSuchAlgorithmException {
		String fileWithKeys;
		String signatureName = signature.getClass().getName();
		// for the empty signature algorithm, the actual keys are irrelevant
		if (signatureName.endsWith("ED25519") || signatureName.endsWith("EMPTY"))
			fileWithKeys = "gameteED25519.keys";
		else if (signatureName.endsWith("SHA256DSA"))
			fileWithKeys = "gameteSHA256DSA.keys";
		else if (signatureName.endsWith("QTESLA"))
			fileWithKeys = "gameteQTesla.keys";
		else
			throw new NoSuchAlgorithmException("I have no keys for signing algorithm " + signatureName);

		if (!signatureName.endsWith("EMPTY"))
			System.out.println("Reading keys of gamete from file: " + fileWithKeys);

		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileWithKeys))) {
			return (KeyPair) ois.readObject();
		}
	}
	private static void initializeNodeIfNeeded() throws TransactionRejectedException, TransactionException,
			CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException {

		// the gamete has both red and green coins, enough for all tests
		KeyPair keysOfGamete = loadKeysOfGamete();

		try {
			originalView.getManifest();
			// the node is already initialized: we use the default gamete,
			// which is expected to be the same used by the node
			gamete = DEFAULT_GAMETE;
		}
		catch (NoSuchElementException e) {
			// if the original node has no manifest yet, it means that it is not initialized and we initialize it
			InitializedNode initialized = InitializedNode.of
				(originalView, keysOfGamete, Paths.get("../modules/explicit/io-takamaka-code-" + version + ".jar"),
					Constants.MANIFEST_NAME, chainId, BigInteger.valueOf(999_999_999).pow(5), BigInteger.valueOf(999_999_999).pow(5));

			gamete = initialized.gamete();
			System.out.println("Initialized the node for testing, with the following gamete: ");
			System.out.println("  " + gamete);
			System.out.println("Use that as " + TakamakaTest.class.getName() +".DEFAULT_GAMETE if you want to run other tests against this same node");
		}

		privateKeyOfGamete = keysOfGamete.getPrivate();
	}

	@SuppressWarnings("unused")
	private static Node mkTendermintBlockchain() {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder().build();
		originalConfig = config;
		return io.hotmoka.tendermint.TendermintBlockchain.of(config);
	}

	@SuppressWarnings("unused")
	private static Node mkMemoryBlockchain() {
		// specify the signing algorithm, if you need; otherwise ED25519 will be used by default
		MemoryBlockchainConfig config = new MemoryBlockchainConfig.Builder().build();
		//MemoryBlockchainConfig config = new MemoryBlockchainConfig.Builder().signRequestsWith("qtesla").build();
		originalConfig = config;
		return io.hotmoka.memory.MemoryBlockchain.of(config);
	}

	@SuppressWarnings("unused")
	private static Node mkTakamakaBlockchainExecuteOneByOne() {
		TakamakaBlockchainConfig config = new TakamakaBlockchainConfig.Builder()
			.allowSelfCharged(true)
			.build();
		originalConfig = config;
		return TakamakaBlockchainOneByOne.takamakaBlockchain = TakamakaBlockchain.of(config, TakamakaBlockchainOneByOne::postTransactionTakamakaBlockchainRequestsOneByOne);
	}

	// this code must stay in its own class, or otherwise the static initialization of TakamakaTest goes
	// into an infinite loop!
	private static class TakamakaBlockchainOneByOne {
		/**
		 * Only used for testing with this blockchain.
		 */
		private static TakamakaBlockchain takamakaBlockchain;
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
		/**
		 * Only used for testing with this blockchain.
		 */
		private static TakamakaBlockchain takamakaBlockchain;
		private static List<TransactionRequest<?>> mempool = new ArrayList<>();

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
	private static Node mkTakamakaBlockchainExecuteAtEachTimeslot() {
		TakamakaBlockchainConfig config = new TakamakaBlockchainConfig.Builder()
			.allowSelfCharged(true)
			.build();
		originalConfig = config;
		List<TransactionRequest<?>> mempool = TakamakaBlockchainAtEachTimeslot.mempool;

		// we provide an implementation of postTransaction() that just adds the request in the mempool
		TakamakaBlockchainAtEachTimeslot.takamakaBlockchain = TakamakaBlockchain.of(config, TakamakaBlockchainAtEachTimeslot::postTransactionTakamakaBlockchainRequestsOneByOne);

		// we start a scheduler that checks the mempool every timeslot to see if there are requests to execute
		Thread scheduler = new Thread() {

			@Override
			public void run() {
				byte[] hash = null;
				TakamakaBlockchain node = TakamakaBlockchainAtEachTimeslot.takamakaBlockchain;

				while (true) {
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {}

					// we check if a previous execute() is still running,
					// since we cannot run two execute() at the same time
					if (node.getCurrentExecutionId().isEmpty()) {
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

						DeltaGroupExecutionResult result = node.execute(hash, System.currentTimeMillis(), requests, Stream.generate(() -> BigInteger.ZERO).limit(size), "id");
						hash = result.getHash();
						node.checkOut(hash);
					}
				}
			}
		};

		scheduler.start();

		logger.info("scheduled mempool check every 100 milliseconds");
		return TakamakaBlockchainAtEachTimeslot.takamakaBlockchain;
	}

	@SuppressWarnings("unused")
	private static Node mkRemoteNode(Node exposed) throws Exception {
		// we use port 8080, so that it does not interfere with the other service opened at port 8081 by the network tests
		NodeServiceConfig serviceConfig = new NodeServiceConfig.Builder()
			.setPort(8080)
			.setSpringBannerModeOn(false).build();

		NodeService.of(serviceConfig, exposed);

		RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder()
			//.setWebSockets(false).setURL("localhost:8080")
			// uncomment for using websockets
			//.setWebSockets(true).setURL("localhost:8080")
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

	protected final void setNode(BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		nodeWithJarsView = null;
		nodeWithAccountsView = NodeWithAccounts.of(originalView, localGamete, privateKeyOfLocalGamete, coins);
	}

	protected final void setNodeRedGreen(BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		nodeWithJarsView = null;
		nodeWithAccountsView = NodeWithAccounts.ofRedGreen(originalView, localGamete, privateKeyOfLocalGamete, coins);
	}

	protected final void setNode(String jar, BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		nodeWithJarsView = NodeWithJars.of(originalView, localGamete, privateKeyOfLocalGamete, pathOfExample(jar));
		nodeWithAccountsView = NodeWithAccounts.of(originalView, localGamete, privateKeyOfLocalGamete, coins);
	}

	protected final void setNodeRedGreen(String jar, BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		nodeWithJarsView = NodeWithJars.of(originalView, localGamete, privateKeyOfLocalGamete, pathOfExample(jar));
		nodeWithAccountsView = NodeWithAccounts.ofRedGreen(originalView, localGamete, privateKeyOfLocalGamete, coins);
	}

	protected final TransactionReference takamakaCode() {
		return nodeWithAccountsView.getTakamakaCode();
	}

	protected final TransactionReference jar() {
		return nodeWithJarsView.jar(0);
	}

	protected final StorageReference account(int i) {
		return nodeWithAccountsView.account(i);
	}

	protected final PrivateKey privateKey(int i) {
		return nodeWithAccountsView.privateKey(i);
	}

	protected final SignatureAlgorithm<NonInitialTransactionRequest<?>> signature() throws NoSuchAlgorithmException {
		return signature;
	}

	protected final TransactionRequest<?> getRequest(TransactionReference reference) {
		return originalView.getRequest(reference);
	}

	protected final TransactionResponse getResponse(TransactionReference reference) throws NoSuchElementException, TransactionRejectedException {
		return originalView.getResponse(reference);
	}

	protected final TransactionReference addJarStoreInitialTransaction(byte[] jar, TransactionReference... dependencies) throws TransactionException, TransactionRejectedException {
		return nodeWithAccountsView.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final TransactionReference addJarStoreTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.addJarStoreTransaction(new JarStoreTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageReference addConstructorCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.addConstructorCallTransaction(new ConstructorCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, constructor, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue addInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue addStaticMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, method, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(Signer.with(signature, key), caller, BigInteger.ZERO, chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runStaticMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.runStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(Signer.with(signature, key), caller, BigInteger.ZERO, chainId, gasLimit, gasPrice, classpath, method, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final JarSupplier postJarStoreTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.postJarStoreTransaction(new JarStoreTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeSupplier<StorageValue> postInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeSupplier<StorageReference> postConstructorCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.postConstructorCallTransaction(new ConstructorCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, constructor, actuals));
	}

	protected static byte[] bytesOf(String fileName) throws IOException {
		return Files.readAllBytes(pathOfExample(fileName));
	}

	protected static Path pathOfExample(String fileName) {
		return Paths.get("../io-takamaka-examples/target/io-takamaka-examples-" + version + '-' + fileName);
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

	/**
	 * Gets the nonce of the given account. It calls the {@code Account.nonce()} method.
	 * 
	 * @param account the account
	 * @param key the private key of the account
	 * @return the nonce
	 * @throws TransactionException if the nonce cannot be found
	 */
	private BigInteger getNonceOf(StorageReference account, PrivateKey key) throws TransactionRejectedException {
		try {
			BigInteger nonce = nonces.get(account);
			if (nonce != null)
				nonce = nonce.add(BigInteger.ONE);
			else
				// we ask the account: 10,000 units of gas should be enough to run the method
				nonce = ((BigIntegerValue) nodeWithAccountsView.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(Signer.with(signature, key), account, BigInteger.ZERO, "", BigInteger.valueOf(10_000), BigInteger.ZERO, nodeWithAccountsView.getClassTag(account).jar, new NonVoidMethodSignature(Constants.ACCOUNT_NAME, "nonce", ClassType.BIG_INTEGER), account))).value;

			nonces.put(account, nonce);
			return nonce;
		}
		catch (Exception e) {
			logger.error("failed computing nonce", e);
			throw new TransactionRejectedException("cannot compute the nonce of " + account);
		}
	}
}