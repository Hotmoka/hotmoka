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
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.NodeInfos;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.nodes.NodeInfo;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithEvents;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.constants.Constants;
import io.hotmoka.node.api.SimpleValidatorsConsensusConfig;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.tendermint.api.TendermintNode;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.tendermint.abci.Server;

/**
 * An implementation of a node working over the Tendermint generic blockchain engine.
 * Requests sent to this node are forwarded to a Tendermint process. This process
 * checks and delivers such requests, by calling the ABCI interface. This blockchain keeps
 * its state in a transactional database implemented by the {@link Store} class.
 */
@ThreadSafe
public class TendermintNodeImpl extends AbstractLocalNode<TendermintNodeConfig, Store> implements TendermintNode {

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
	 * The time to use for the currently executing transaction.
	 */
	private volatile long now;

	/**
	 * Builds a brand new Tendermint blockchain. This constructor spawns the Tendermint process on localhost
	 * and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 * @param consensus the consensus parameters of the node
	 * @throws IOException 
	 */
	public TendermintNodeImpl(TendermintNodeConfig config, SimpleValidatorsConsensusConfig consensus) throws IOException {
		super(config, consensus);

		try {
			this.isWindows = System.getProperty("os.name").startsWith("Windows");
			initWorkingDirectoryOfTendermintProcess(config);
			var tendermintConfigFile = new TendermintConfigFile(config);
			this.abci = new Server(tendermintConfigFile.abciPort, new TendermintApplication(new TendermintBlockchainInternalImpl()));
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
			TendermintConfigFile tendermintConfigFile = new TendermintConfigFile(config);
			this.abci = new Server(tendermintConfigFile.abciPort, new TendermintApplication(new TendermintBlockchainInternalImpl()));
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
		return NodeInfos.of(TendermintNode.class.getName(), Constants.HOTMOKA_VERSION, poster.getNodeID());
	}

	@Override
	public TendermintNodeConfig getConfig() {
		return config;
	}

	@Override
	protected Store mkStore() {
		return new Store(caches::getResponseUncommitted, config.getDir(), new TendermintBlockchainInternalImpl());
	}

	@Override
	protected long getNow() {
		return now;
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
	private static final ClassType storageMapView = StorageTypes.classNamed("io.takamaka.code.util.StorageMapView");
	private static final MethodSignature SIZE = new NonVoidMethodSignature(storageMapView, "size", StorageTypes.INT);
	private static final MethodSignature GET_SHARES = new NonVoidMethodSignature(StorageTypes.VALIDATORS, "getShares", storageMapView);
	private static final MethodSignature SELECT = new NonVoidMethodSignature(storageMapView, "select", StorageTypes.OBJECT, StorageTypes.INT);
	private static final MethodSignature GET = new NonVoidMethodSignature(storageMapView, "get", StorageTypes.OBJECT, StorageTypes.OBJECT);

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
			(manifest, _50_000, takamakaCode, SIZE, shares))).getValue();

		TendermintValidator[] result = new TendermintValidator[numOfValidators];

		for (int num = 0; num < numOfValidators; num++) {
			StorageReference validator = (StorageReference) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _50_000, takamakaCode, SELECT, shares, StorageValues.intOf(num)));

			String id = ((StringValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _50_000, takamakaCode, MethodSignatures.ID, validator))).getValue();

			long power = ((BigIntegerValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _50_000, takamakaCode, GET, shares, validator))).getValue().longValue();

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
		if (storeUtilities.nodeIsInitializedUncommitted() && response instanceof TransactionResponseWithEvents) {
			Stream<StorageReference> events = ((TransactionResponseWithEvents) response).getEvents();
			StorageReference validators = caches.getValidators().get();

			return check(ClassNotFoundException.class, () ->
				events.filter(uncheck(event -> isValidatorsUpdateEvent(event, classLoader)))
					.map(storeUtilities::getCreatorUncommitted)
					.anyMatch(validators::equals)
			);
		}

		return false;
	}

	private boolean isValidatorsUpdateEvent(StorageReference event, EngineClassLoader classLoader) throws ClassNotFoundException {
		return classLoader.isValidatorsUpdateEvent(storeUtilities.getClassNameUncommitted(event));
	}

	private class TendermintBlockchainInternalImpl implements TendermintNodeInternal {

		@Override
		public TendermintNodeConfig getConfig() {
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
			return TendermintNodeImpl.this.trimmedMessage(t);
		}

		@Override
		public void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
			TendermintNodeImpl.this.checkTransaction(request);
		}

		@Override
		public TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
			return TendermintNodeImpl.this.deliverTransaction(request);
		}

		@Override
		public Optional<TendermintValidator[]> getTendermintValidatorsInStore() throws TransactionRejectedException, TransactionException, CodeExecutionException {
			return TendermintNodeImpl.this.getTendermintValidatorsInStore();
		}

		@Override
		public void commitTransactionAndCheckout() {
			TendermintNodeImpl.this.commitTransactionAndCheckout();
		}

		@Override
		public boolean rewardValidators(String behaving, String misbehaving) {
			return TendermintNodeImpl.this.rewardValidators(behaving, misbehaving);
		}

		@Override
		public void setNow(long now) {
			TendermintNodeImpl.this.now = now;
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
}