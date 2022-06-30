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

package io.hotmoka.tendermint.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.nodes.NodeInfo;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithEvents;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.constants.Constants;
import io.hotmoka.local.AbstractLocalNode;
import io.hotmoka.local.EngineClassLoader;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.TendermintValidator;
import io.hotmoka.tendermint_abci.Server;

/**
 * An implementation of a blockchain working over the Tendermint generic blockchain engine.
 * Requests sent to this blockchain are forwarded to a Tendermint process. This process
 * checks and delivers such requests, by calling the ABCI interface. This blockchain keeps
 * its state in a transactional database implemented by the {@linkplain Store} class.
 */
@ThreadSafe
public class TendermintBlockchainImpl extends AbstractLocalNode<TendermintBlockchainConfig, Store> implements TendermintBlockchain {

	private final static Logger logger = Logger.getLogger(TendermintBlockchainImpl.class.getName());

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
	 * Builds a brand new Tendermint blockchain. This constructor spawns the Tendermint process on localhost
	 * and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 * @param consensus the consensus parameters of the node
	 * @throws NoSuchFileException if some configuration file does not exist
	 */
	public TendermintBlockchainImpl(TendermintBlockchainConfig config, ConsensusParams consensus) throws NoSuchFileException {
		super(config, consensus);

		try {
			this.isWindows = System.getProperty("os.name").startsWith("Windows");
			initWorkingDirectoryOfTendermintProcess(config);
			TendermintConfigFile tendermintConfigFile = new TendermintConfigFile(config);
			this.abci = new Server(tendermintConfigFile.abciPort, new TendermintApplication(new TendermintBlockchainInternalImpl()));
			this.abci.start();
			logger.info("ABCI started at port " + tendermintConfigFile.abciPort);
			this.poster = new TendermintPoster(config, tendermintConfigFile.tendermintPort);
			this.tendermint = new Tendermint(config);
			logger.info("Tendermint started at port " + tendermintConfigFile.tendermintPort);
		}
		catch (NoSuchFileException e) {
			logger.log(Level.SEVERE, "the creation of the Tendermint blockchain failed", e);
			tryClose();
			throw e;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "the creation of the Tendermint blockchain failed", e);
			tryClose();
			throw InternalFailureException.of(e);
		}
	}

	private void tryClose() {
		try {
			close();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "cannot close the blockchain", e);
		}
	}

	/**
	 * Builds a Tendermint blockchain recycling the previous store. The consensus parameters
	 * are recovered from the manifest in the store. This constructor spawns the Tendermint process on localhost
	 * and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 */
	public TendermintBlockchainImpl(TendermintBlockchainConfig config) {
		super(config);

		try {
			this.isWindows = System.getProperty("os.name").startsWith("Windows");
			TendermintConfigFile tendermintConfigFile = new TendermintConfigFile(config);
			this.abci = new Server(tendermintConfigFile.abciPort, new TendermintApplication(new TendermintBlockchainInternalImpl()));
			this.abci.start();
			logger.info("ABCI started at port " + tendermintConfigFile.abciPort);
			this.poster = new TendermintPoster(config, tendermintConfigFile.tendermintPort);
			this.tendermint = new Tendermint(config);
			logger.info("Tendermint started at port " + tendermintConfigFile.tendermintPort);
			caches.recomputeConsensus();
		}
		catch (Exception e) {// we check if there are events of type ValidatorsUpdate triggered by validators
			logger.log(Level.SEVERE, "the creation of the Tendermint blockchain failed", e);

			tryClose();

			throw InternalFailureException.of(e);
		}
	}

	@Override
	public void close() throws Exception {
		if (isNotYetClosed()) {
			super.close();

			if (tendermint != null)
				tendermint.close();

			if (abci != null && !abci.isShutdown()) {
				abci.shutdown();
				abci.awaitTermination();
			}
		}
	}

	@Override
	public NodeInfo getNodeInfo() {
		return new NodeInfo(TendermintBlockchain.class.getName(), Constants.VERSION, poster.getNodeID());
	}

	@Override
	public TendermintBlockchainConfig getConfig() {
		return config;
	}

	@Override
	protected Store mkStore() {
		return new Store(this, new TendermintBlockchainInternalImpl());
	}

	@Override
	protected void postRequest(TransactionRequest<?> request) {
		poster.postRequest(request);
	}

	@Override
	protected void invalidateCachesIfNeeded(TransactionResponse response, EngineClassLoader classLoader) {
		super.invalidateCachesIfNeeded(response, classLoader);
	
		if (validatorsMightHaveChanged(response, classLoader)) {
			tendermintValidatorsCached = null;
			logger.info("the validators set has been invalidated since their information might have changed");
		}
	}

	@Override
	protected void scheduleForNotificationOfEvents(TransactionResponseWithEvents response) {
		responsesWithEventsToNotify.add(response);
	}

	/**
	 * The transactions containing events that must be notified at the next commit.
	 */
	private final Set<TransactionResponseWithEvents> responsesWithEventsToNotify = new HashSet<>();

	private void commitTransactionAndCheckout() {
		store.commitTransactionAndCheckout();
		responsesWithEventsToNotify.forEach(this::notifyEventsOf);
		responsesWithEventsToNotify.clear();
	}

	private static final BigInteger _50_000 = BigInteger.valueOf(50_000);
	private static final ClassType storageMapView = new ClassType("io.takamaka.code.util.StorageMapView");
	private static final MethodSignature SIZE = new NonVoidMethodSignature(storageMapView, "size", BasicTypes.INT);
	private static final MethodSignature GET_SHARES = new NonVoidMethodSignature(ClassType.VALIDATORS, "getShares", storageMapView);
	private static final MethodSignature SELECT = new NonVoidMethodSignature(storageMapView, "select", ClassType.OBJECT, BasicTypes.INT);
	private static final MethodSignature GET = new NonVoidMethodSignature(storageMapView, "get", ClassType.OBJECT, ClassType.OBJECT);

	private volatile TendermintValidator[] tendermintValidatorsCached;

	private Optional<TendermintValidator[]> getTendermintValidatorsInStore() throws TransactionRejectedException, TransactionException, CodeExecutionException {
		if (tendermintValidatorsCached != null)
			return Optional.of(tendermintValidatorsCached);

		StorageReference manifest;

		try {
			manifest = getManifest();
		}
		catch (NoSuchElementException e) {
			return Optional.empty();
		}

		StorageReference validators = caches.getValidators().get(); // the manifest is already set
		TransactionReference takamakaCode = getTakamakaCode();

		StorageReference shares = (StorageReference) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _50_000, takamakaCode, GET_SHARES, validators));

		int numOfValidators = ((IntValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _50_000, takamakaCode, SIZE, shares))).value;

		TendermintValidator[] result = new TendermintValidator[numOfValidators];

		for (int num = 0; num < numOfValidators; num++) {
			StorageReference validator = (StorageReference) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _50_000, takamakaCode, SELECT, shares, new IntValue(num)));

			String id = ((StringValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _50_000, takamakaCode, CodeSignature.ID, validator))).value;

			long power = ((BigIntegerValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _50_000, takamakaCode, GET, shares, validator))).value.longValue();

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
	 */
	private boolean validatorsMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) {
		if (storeUtilities.nodeIsInitializedUncommitted() && response instanceof TransactionResponseWithEvents) {
			Stream<StorageReference> events = ((TransactionResponseWithEvents) response).getEvents();
			StorageReference validators = caches.getValidators().get();

			return events.filter(event -> isValidatorsUpdateEvent(event, classLoader))
				.map(storeUtilities::getCreatorUncommitted)
				.anyMatch(validators::equals);
		}

		return false;
	}

	private boolean isValidatorsUpdateEvent(StorageReference event, EngineClassLoader classLoader) {
		return classLoader.isValidatorsUpdateEvent(storeUtilities.getClassNameUncommitted(event));
	}

	private class TendermintBlockchainInternalImpl implements TendermintBlockchainInternal {

		@Override
		public TendermintBlockchainConfig getConfig() {
			return config;
		}

		@Override
		public Store getStore() {
			return store;
		}

		@Override
		public TendermintPoster getPoster() {
			return poster;
		}

		@Override
		public String trimmedMessage(Throwable t) {
			return TendermintBlockchainImpl.this.trimmedMessage(t);
		}

		@Override
		public void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
			TendermintBlockchainImpl.this.checkTransaction(request);
		}

		@Override
		public TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
			return TendermintBlockchainImpl.this.deliverTransaction(request);
		}

		@Override
		public Optional<TendermintValidator[]> getTendermintValidatorsInStore() throws TransactionRejectedException, TransactionException, CodeExecutionException {
			return TendermintBlockchainImpl.this.getTendermintValidatorsInStore();
		}

		@Override
		public void commitTransactionAndCheckout() {
			TendermintBlockchainImpl.this.commitTransactionAndCheckout();
		}

		@Override
		public boolean rewardValidators(String behaving, String misbehaving) {
			return TendermintBlockchainImpl.this.rewardValidators(behaving, misbehaving);
		}
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
		Tendermint(TendermintBlockchainConfig config) throws IOException, InterruptedException, TimeoutException {
			this.process = spawnTendermintProcess(config);
			waitUntilTendermintProcessIsUp(config);

			logger.info("The Tendermint process is up and running");
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
					logger.info(br.lines().collect(Collectors.joining()));
				}

			logger.info("The Tendermint process has been shut down");
		}

		/**
		 * Spawns a Tendermint process using the working directory that has been previously initialized.
		 * 
		 * @param config the configuration of the node
		 * @return the Tendermint process
		 */
		private Process spawnTendermintProcess(TendermintBlockchainConfig config) throws IOException {
			// spawns a process that remains in background
			Path tendermintHome = config.dir.resolve("blocks");
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
		private void waitUntilTendermintProcessIsUp(TendermintBlockchainConfig config) throws TimeoutException, InterruptedException, IOException {
			for (int reconnections = 1; reconnections <= config.maxPingAttempts; reconnections++) {
				try {
					HttpURLConnection connection = poster.openPostConnectionToTendermint();
					try (OutputStream os = connection.getOutputStream(); InputStream is = connection.getInputStream()) {
						return;
					}
				}
				catch (ConnectException e) {
					// take a nap, then try again
					Thread.sleep(config.pingDelay);
				}
			}

			try {
				close();
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, "Cannot close the Tendermint process", e);
			}

			throw new TimeoutException("cannot connect to Tendermint process at " + poster.url() + ". Tried " + config.maxPingAttempts + " times");
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
	private void initWorkingDirectoryOfTendermintProcess(TendermintBlockchainConfig config) throws InterruptedException, IOException {
		if (config.tendermintConfigurationToClone == null) {
			// if there is no configuration to clone, we create a default network of a single node
			// that plays the role of unique validator of the network

			Path tendermintHome = config.dir.resolve("blocks");
			String executableName = isWindows ? "cmd.exe /c tendermint.exe" : "tendermint";
			//if (run("tendermint testnet --v 1 --o " + tendermintHome + " --populate-persistent-peers", Optional.empty()).waitFor() != 0)
			if (run(executableName + " init --home " + tendermintHome, Optional.empty()).waitFor() != 0)
				throw new IOException("Tendermint initialization failed");
		}
		else
			// we clone the configuration files inside config.tendermintConfigurationToClone
			// into the blocks subdirectory of the node directory
			copyRecursively(config.tendermintConfigurationToClone, config.dir.resolve("blocks"));
	}
}