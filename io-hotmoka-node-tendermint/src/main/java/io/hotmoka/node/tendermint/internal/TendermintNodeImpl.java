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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonSyntaxException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.local.AbstractCheckableLocalNode;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.tendermint.api.TendermintNode;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.tendermint.abci.Server;

/**
 * An implementation of a node working over the Tendermint generic blockchain engine.
 * Requests sent to this node are forwarded to a Tendermint process. This process
 * checks and delivers such requests, by calling the ABCI interface. This blockchain keeps
 * its state in a transactional database implemented by the {@link TendermintStore} class.
 */
@ThreadSafe
public class TendermintNodeImpl extends AbstractCheckableLocalNode<TendermintNodeConfig, TendermintStore, TendermintStoreTransformation> implements TendermintNode {

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
	 * Builds a brand new Tendermint blockchain. This constructor spawns the Tendermint process on localhost
	 * and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 * @param consensus the consensus parameters of the node
	 * @throws NodeException if the operation cannot be completed correctly
	 * @throws InterruptedException the the currently thread is interrupted before completing the construction
	 */
	public TendermintNodeImpl(TendermintNodeConfig config, Optional<ConsensusConfig<?,?>> consensus) throws NodeException, InterruptedException {
		super(consensus, config);

		try {
			this.isWindows = System.getProperty("os.name").startsWith("Windows");
			initStore(consensus);
			if (consensus.isPresent())
				initWorkingDirectoryOfTendermintProcess(config);
			var tendermintConfigFile = new TendermintConfigFile(config);
			this.poster = new TendermintPoster(config, tendermintConfigFile.tendermintPort);
			this.abci = new Server(tendermintConfigFile.abciPort, new TendermintApplication(this, poster));
			this.abci.start();
			LOGGER.info("Tendermint ABCI started at port " + tendermintConfigFile.abciPort);
			this.tendermint = new Tendermint(config);
			LOGGER.info("Tendermint started at port " + tendermintConfigFile.tendermintPort);
		}
		catch (IOException | TimeoutException e) {
			LOGGER.log(Level.SEVERE, "the creation of the Tendermint node failed. Is Tendermint installed?", e);
			close();
			throw new NodeException("The creation of the Tendermint node failed. Is Tendermint installed?", e);
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

	protected final long getBlockHeight() throws NodeException {
		try {
			return getStore().getHeight();
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	protected final byte[] getTendermintHash() throws NodeException {
		try {
			return getStore().getTendermintHash();
		}
		catch (StoreException e) {
			throw new NodeException(e);
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

	@Override
	protected TendermintStore mkStore(ExecutorService executors, ConsensusConfig<?,?> consensus, TendermintNodeConfig config, Hasher<TransactionRequest<?>> hasher) throws NodeException {
		try {
			return new TendermintStore(getEnvironment(), executors, consensus, config, hasher);
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected void postRequest(TransactionRequest<?> request) throws NodeException, TimeoutException, InterruptedException {
		poster.postRequest(request);
	}

	@Override
	protected TendermintStoreTransformation beginTransaction(long now) throws NodeException {
		return super.beginTransaction(now);
	}

	@Override
	protected void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException, NodeException {
		super.checkTransaction(request);
	}

	@Override
	protected void signalRejected(TransactionRequest<?> request, TransactionRejectedException e) {
		super.signalRejected(request, e);
	}

	@Override
	protected void moveToFinalStoreOf(TendermintStoreTransformation transaction) throws NodeException {
		super.moveToFinalStoreOf(transaction);
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
		var processBuilder = new ProcessBuilder();
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
	 * @throws NodeException 
	 */
	private void initWorkingDirectoryOfTendermintProcess(TendermintNodeConfig config) throws InterruptedException, NodeException {
		Optional<Path> tendermintConfigurationToClone = config.getTendermintConfigurationToClone();
		Path tendermintHome = config.getDir().resolve("blocks");

		try {
			if (tendermintConfigurationToClone.isEmpty()) {
				// if there is no configuration to clone, we create a default network of a single node
				// that plays the role of the unique validator of the network
				String executableName = isWindows ? "cmd.exe /c tendermint.exe" : "tendermint";
				//if (run("tendermint testnet --v 1 --o " + tendermintHome + " --populate-persistent-peers", Optional.empty()).waitFor() != 0)
				if (run(executableName + " init --home " + tendermintHome, Optional.empty()).waitFor() != 0) // TODO: add timeout
					throw new NodeException("Tendermint initialization failed");
			}
			else
				// we clone the configuration files inside config.tendermintConfigurationToClone
				// into the blocks subdirectory of the node directory
				copyRecursively(tendermintConfigurationToClone.get(), tendermintHome);
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * A proxy object that connects to the Tendermint process, sends requests to it
	 * and gets responses from it.
	 */
	private class Tendermint implements AutoCloseable {
	
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
		private Tendermint(TendermintNodeConfig config) throws IOException, InterruptedException, TimeoutException {
			this.process = spawnTendermintProcess(config);
			waitUntilTendermintProcessIsUp(config);
	
			LOGGER.info("the Tendermint process is up and running");
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
	
			LOGGER.info("the Tendermint process has been shut down");
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
				LOGGER.log(Level.SEVERE, "cannot close the Tendermint process", e);
			}
	
			throw new TimeoutException("Cannot connect to Tendermint process at " + poster.url() + ". Tried " + config.getMaxPingAttempts() + " times");
		}
	}
}