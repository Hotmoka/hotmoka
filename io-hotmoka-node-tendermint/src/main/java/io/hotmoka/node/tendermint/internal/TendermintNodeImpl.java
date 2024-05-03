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

package io.hotmoka.node.tendermint.internal;

import static io.hotmoka.exceptions.CheckSupplier.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonSyntaxException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.UninitializedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.nodes.ValidatorsConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.tendermint.api.TendermintNode;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.stores.StoreException;
import io.hotmoka.stores.StoreTransaction;
import io.hotmoka.tendermint.abci.Server;

/**
 * An implementation of a node working over the Tendermint generic blockchain engine.
 * Requests sent to this node are forwarded to a Tendermint process. This process
 * checks and delivers such requests, by calling the ABCI interface. This blockchain keeps
 * its state in a transactional database implemented by the {@link TendermintStore} class.
 */
@ThreadSafe
public class TendermintNodeImpl extends AbstractLocalNode<TendermintNodeConfig, TendermintStore> implements TendermintNode {

	private final static Logger LOGGER = Logger.getLogger(TendermintNodeImpl.class.getName());

	/**
	 * The GRPC server that runs the ABCI process.
	 */
	private final Server abci;

	/**
	 * A proxy to the Tendermint process.
	 */
	private final Tendermint tendermint;

	/**
	 * An object for posting requests to the Tendermint process.
	 */
	private final TendermintPoster poster;

	/**
	 * True if and only if we are running on Windows.
	 */
	private final boolean isWindows;

	/**
	 * The current store transaction.
	 */
	public volatile StoreTransaction<TendermintStore> transaction;

	/**
	 * Builds a brand new Tendermint blockchain. This constructor spawns the Tendermint process on localhost
	 * and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 * @param consensus the consensus parameters of the node
	 * @throws IOException 
	 */
	public TendermintNodeImpl(TendermintNodeConfig config, ValidatorsConsensusConfig<?,?> consensus) throws IOException {
		super(config, consensus);

		try {
			this.isWindows = System.getProperty("os.name").startsWith("Windows");
			initWorkingDirectoryOfTendermintProcess(config);
			var tendermintConfigFile = new TendermintConfigFile(config);
			this.abci = new Server(tendermintConfigFile.abciPort, new TendermintApplication(this));
			this.abci.start();
			LOGGER.info("ABCI started at port " + tendermintConfigFile.abciPort);
			this.poster = new TendermintPoster(config, tendermintConfigFile.tendermintPort);
			this.tendermint = new Tendermint(config);
			LOGGER.info("Tendermint started at port " + tendermintConfigFile.tendermintPort);
		}
		catch (NoSuchFileException e) {
			LOGGER.log(Level.SEVERE, "the creation of the Tendermint blockchain failed", e);
			tryClose();
			throw e;
		}
		catch (TimeoutException | InterruptedException e) {
			LOGGER.log(Level.SEVERE, "the creation of the Tendermint blockchain failed. Is Tendermint installed?", e);
			tryClose();
			throw new RuntimeException("unexpected exception", e);
		}
	}

	private void tryClose() {
		try {
			close();
		}
		catch (Exception e) {
			LOGGER.log(Level.SEVERE, "cannot close the blockchain", e);
		}
	}

	/**
	 * Builds a Tendermint blockchain recycling the previous store. The consensus parameters
	 * are recovered from the manifest in the store. This constructor spawns the Tendermint process on localhost
	 * and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 * @throws IOException 
	 */
	public TendermintNodeImpl(TendermintNodeConfig config) throws IOException {
		super(config);

		try {
			this.isWindows = System.getProperty("os.name").startsWith("Windows");
			var tendermintConfigFile = new TendermintConfigFile(config);
			this.abci = new Server(tendermintConfigFile.abciPort, new TendermintApplication(this));
			this.abci.start();
			LOGGER.info("ABCI started at port " + tendermintConfigFile.abciPort);
			this.poster = new TendermintPoster(config, tendermintConfigFile.tendermintPort);
			this.tendermint = new Tendermint(config);
			LOGGER.info("Tendermint started at port " + tendermintConfigFile.tendermintPort);
			caches.recomputeConsensus();
		}
		catch (TimeoutException | InterruptedException e) {
			LOGGER.log(Level.SEVERE, "the creation of the Tendermint blockchain failed. Is Tendermint installed?", e);
			tryClose();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void closeResources() throws NodeException, InterruptedException {
		try {
			closeTendermintAndABCI();
		}
		finally {
			super.closeResources();
		}
	}

	private void closeTendermintAndABCI() throws NodeException, InterruptedException {
		try {
			if (tendermint != null)
				tendermint.close();
		}
		catch (IOException e) {
			throw new NodeException("Could not close Tendermint", e);
		}
		finally {
			closeABCI();
		}
	}

	private void closeABCI() throws InterruptedException {
		if (abci != null && !abci.isShutdown()) {
			abci.shutdown();
			abci.awaitTermination();
		}		
	}

	@Override
	public NodeInfo getNodeInfo() throws NodeException, TimeoutException, InterruptedException {
		try (var scope = mkScope()) {
			return NodeInfos.of(TendermintNode.class.getName(), HOTMOKA_VERSION, poster.getNodeID());
		}
		catch (JsonSyntaxException | IOException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public TendermintNodeConfig getLocalConfig() {
		return config;
	}

	public TendermintPoster getPoster() {
		return poster;
	}

	@Override
	protected TendermintStore mkStore() {
		return new TendermintStore(config.getDir(), this);
	}

	@Override
	protected void postRequest(TransactionRequest<?> request) {
		poster.postRequest(request);
	}

	@Override
	protected void invalidateCachesIfNeeded(TransactionResponse response, EngineClassLoader classLoader) throws ClassNotFoundException {
		super.invalidateCachesIfNeeded(response, classLoader);
	
		if (validatorsMightHaveChanged(response, classLoader)) {
			tendermintValidatorsCached = null;
			LOGGER.info("the validators set has been invalidated since their information might have changed");
		}
	}

	private static final BigInteger _50_000 = BigInteger.valueOf(50_000);
	private static final ClassType storageMapView = StorageTypes.classNamed("io.takamaka.code.util.StorageMapView");
	private static final MethodSignature SIZE = MethodSignatures.ofNonVoid(storageMapView, "size", StorageTypes.INT);
	private static final MethodSignature GET_SHARES = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getShares", storageMapView);
	private static final MethodSignature SELECT = MethodSignatures.ofNonVoid(storageMapView, "select", StorageTypes.OBJECT, StorageTypes.INT);
	private static final MethodSignature GET = MethodSignatures.ofNonVoid(storageMapView, "get", StorageTypes.OBJECT, StorageTypes.OBJECT);

	private volatile TendermintValidator[] tendermintValidatorsCached;

	Optional<TendermintValidator[]> getTendermintValidatorsInStore() throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException {
		if (tendermintValidatorsCached != null)
			return Optional.of(tendermintValidatorsCached);

		StorageReference manifest;

		try {
			manifest = getManifest();
		}
		catch (UninitializedNodeException e) {
			return Optional.empty();
		}

		StorageReference validators = caches.getValidatorsUncommitted().get(); // the manifest is already set
		TransactionReference takamakaCode = getTakamakaCode();

		var shares = (StorageReference) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _50_000, takamakaCode, GET_SHARES, validators))
			.orElseThrow(() -> new NodeException(GET_SHARES + " should not return void"));

		int numOfValidators = ((IntValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _50_000, takamakaCode, SIZE, shares))
			.orElseThrow(() -> new NodeException(SIZE + " should not return void"))).getValue();

		var result = new TendermintValidator[numOfValidators];

		for (int num = 0; num < numOfValidators; num++) {
			var validator = (StorageReference) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _50_000, takamakaCode, SELECT, shares, StorageValues.intOf(num)))
				.orElseThrow(() -> new NodeException(SELECT + " should not return void"));

			String id = ((StringValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _50_000, takamakaCode, MethodSignatures.ID, validator))
				.orElseThrow(() -> new NodeException(MethodSignatures.ID + " should not return void"))).getValue();

			long power = ((BigIntegerValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _50_000, takamakaCode, GET, shares, validator))
				.orElseThrow(() -> new NodeException(GET + " should not return void"))).getValue().longValue();

			String publicKey = storeUtilities.getPublicKeyUncommitted(validator);

			result[num] = new TendermintValidator(id, power, publicKey, "tendermint/PubKeyEd25519");
		}

		tendermintValidatorsCached = result;

		return Optional.of(result);
	}

	/**
	 * Determines if the given response generated events of type ValidatorsUpdate triggered by validators.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used for the transaction
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if some class cannot be found in the Takamaka program
	 */
	private boolean validatorsMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) throws ClassNotFoundException {
		try {
			if (storeUtilities.nodeIsInitializedUncommitted() && response instanceof TransactionResponseWithEvents) {
				Stream<StorageReference> events = ((TransactionResponseWithEvents) response).getEvents();
				StorageReference validators = caches.getValidatorsUncommitted().get();

				return check(ClassNotFoundException.class, () ->
					events.filter(uncheck(event -> isValidatorsUpdateEvent(event, classLoader)))
					.map(storeUtilities::getCreatorUncommitted)
					.anyMatch(validators::equals)
				);
			}
		}
		catch (StoreException | NodeException e) {
			LOGGER.log(Level.SEVERE, "", e);
		}

		return false;
	}

	private boolean isValidatorsUpdateEvent(StorageReference event, EngineClassLoader classLoader) throws ClassNotFoundException {
		return classLoader.isValidatorsUpdateEvent(storeUtilities.getClassNameUncommitted(event));
	}

	/**
	 * A proxy object that connects to the Tendermint process, sends requests to it
	 * and gets responses from it.
	 */
	class Tendermint implements AutoCloseable {

		/**
		 * The Tendermint process;
		 */
		private final Process process;

		/**
		 * Spawns the Tendermint process and creates a proxy to it. It assumes that
		 * the {@code tendermint} command can be executed from the command path.
		 * 
		 * @param config the configuration of the blockchain that is using Tendermint
		 * @throws IOException if an I/O error occurred
		 * @throws TimeoutException if Tendermint did not spawn up in the expected time
		 * @throws InterruptedException if the current thread was interrupted while waiting for the Tendermint process to run
		 */
		Tendermint(TendermintNodeConfig config) throws IOException, InterruptedException, TimeoutException {
			this.process = spawnTendermintProcess(config);
			waitUntilTendermintProcessIsUp(config);

			LOGGER.info("The Tendermint process is up and running");
		}

		@Override
		public void close() throws InterruptedException, IOException {
			// the following is important under Windows, since the shell script thats starts Tendermint
			// under Windows spawns it as a subprocess
			process.descendants().forEach(ProcessHandle::destroy);
			process.destroy();
			process.waitFor();

			if (isWindows)
				// this seems important under Windows
				try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
					LOGGER.info(br.lines().collect(Collectors.joining()));
				}

			LOGGER.info("The Tendermint process has been shut down");
		}

		/**
		 * Spawns a Tendermint process using the working directory that has been previously initialized.
		 * 
		 * @param config the configuration of the node
		 * @return the Tendermint process
		 */
		private Process spawnTendermintProcess(TendermintNodeConfig config) throws IOException {
			// spawns a process that remains in background
			Path tendermintHome = config.getDir().resolve("blocks");
			String executableName = isWindows ? "cmd.exe /c tendermint.exe" : "tendermint";
			return run(executableName + " node --home " + tendermintHome + " --abci grpc", Optional.of("tendermint.log"));
		}

		/**
		 * Waits until the Tendermint process acknowledges a ping.
		 * 
		 * @param config the configuration of the node
		 * @param poster the object that can be used to post to Tendermint
		 * @throws IOException if it is not possible to connect to the Tendermint process
		 * @throws TimeoutException if tried many times, but never got a reply
		 * @throws InterruptedException if interrupted while pinging
		 */
		private void waitUntilTendermintProcessIsUp(TendermintNodeConfig config) throws TimeoutException, InterruptedException, IOException {
			for (long reconnections = 1; reconnections <= config.getMaxPingAttempts(); reconnections++) {
				try {
					HttpURLConnection connection = poster.openPostConnectionToTendermint();
					try (var os = connection.getOutputStream(); var is = connection.getInputStream()) {
						return;
					}
				}
				catch (ConnectException e) {
					// take a nap, then try again
					Thread.sleep(config.getPingDelay());
				}
			}

			try {
				close();
			}
			catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Cannot close the Tendermint process", e);
			}

			throw new TimeoutException("cannot connect to Tendermint process at " + poster.url() + ". Tried " + config.getMaxPingAttempts() + " times");
		}
	}

	private static void copyRecursively(Path src, Path dest) throws IOException {
	    try (Stream<Path> stream = Files.walk(src)) {
	        stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
	    }
	    catch (UncheckedIOException e) {
	    	throw e.getCause();
	    }
	}

	private static void copy(Path source, Path dest) {
		try {
			Files.copy(source, dest);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Runs the given command in the operating system shell.
	 * 
	 * @param command the command to run, as if in a shell
	 * @param redirection the file into which the standard output of the command must be redirected
	 * @throws IOException if the command cannot be run
	 */
	private Process run(String command, Optional<String> redirection) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command(command.split(" "));
		redirection.map(File::new).ifPresent(processBuilder::redirectOutput);

        return processBuilder.start();
	}

	/**
	 * Initialize the working directory for Tendermint.
	 * If that directory is required to be deleted on start-up (which is the default)
	 * there are two possibilities: either it clones a Tendermint configuration directory specified
	 * in the configuration of the node, or it creates a default Tendermint configuration
	 * with a single node, that acts as unique validator of the network.
	 * 
	 * @param config the configuration of the node
	 */
	private void initWorkingDirectoryOfTendermintProcess(TendermintNodeConfig config) throws InterruptedException, IOException {
		Optional<Path> tendermintConfigurationToClone = config.getTendermintConfigurationToClone();

		if (tendermintConfigurationToClone.isEmpty()) {
			// if there is no configuration to clone, we create a default network of a single node
			// that plays the role of the unique validator of the network

			Path tendermintHome = config.getDir().resolve("blocks");
			String executableName = isWindows ? "cmd.exe /c tendermint.exe" : "tendermint";
			//if (run("tendermint testnet --v 1 --o " + tendermintHome + " --populate-persistent-peers", Optional.empty()).waitFor() != 0)
			if (run(executableName + " init --home " + tendermintHome, Optional.empty()).waitFor() != 0)
				throw new IOException("Tendermint initialization failed");
		}
		else
			// we clone the configuration files inside config.tendermintConfigurationToClone
			// into the blocks subdirectory of the node directory
			copyRecursively(tendermintConfigurationToClone.get(), config.getDir().resolve("blocks"));
	}

	@Override
	public StoreTransaction<TendermintStore> getStoreTransaction() {
		return transaction;
	}
}