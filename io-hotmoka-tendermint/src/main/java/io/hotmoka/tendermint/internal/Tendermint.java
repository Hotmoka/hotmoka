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
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.tendermint.TendermintBlockchainConfig;

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
	 * True if and only if we are running on Windows.
	 */
	private final boolean isWindows;

	private final static Logger logger = LoggerFactory.getLogger(Tendermint.class);

	/**
	 * Spawns the Tendermint process and creates a proxy to it. It assumes that
	 * the {@code tendermint} command can be executed from the command path.
	 * 
	 * @param config the configuration of the blockchain that is using Tendermint
	 * @param deletePrevious true if and only if a previously existing working directory must
	 *                       be deleted and recreated; if false, its content gets recycled
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if Tendermint did not spawn up in the expected time
	 * @throws InterruptedException if the current thread was interrupted while waiting for the Tendermint process to run
	 */
	Tendermint(TendermintBlockchainConfig config, boolean deletePrevious) throws IOException, InterruptedException, TimeoutException {
		isWindows = System.getProperty("os.name").startsWith("Windows");

		if (deletePrevious)
			initWorkingDirectoryOfTendermintProcess(config);

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

			String tendermintHome = config.dir + File.separator + "blocks";
			String executableName = isWindows ? "tendermint.exe" : "tendermint";
			//if (run("tendermint testnet --v 1 --o " + tendermintHome + " --populate-persistent-peers", Optional.empty()).waitFor() != 0)
			if (run(executableName + " init --home " + tendermintHome, Optional.empty()).waitFor() != 0)
				throw new IOException("Tendermint initialization failed");
		}
		else
			// we clone the configuration files inside node.config.tendermintConfigurationToClone
			// into the blocks subdirectory of the node directory
			copyRecursively(config.tendermintConfigurationToClone, config.dir.resolve("blocks"));
	}

	/**
	 * Spawns a Tendermint process using the working directory that has been previously initialized.
	 * 
	 * @param config the configuration of the node
	 * @return the Tendermint process
	 */
	private Process spawnTendermintProcess(TendermintBlockchainConfig config) throws IOException {
		// spawns a process that remains in background
		String tendermintHome = config.dir + File.separator + "blocks";
		String executableName = isWindows ? "tendermint.exe" : "tendermint";
		return run(executableName + " node --home " + tendermintHome + " --abci grpc --proxy_app tcp://127.0.0.1:" + config.abciPort, Optional.of("tendermint.log"));
	}

	/**
	 * Waits until the Tendermint process acknowledges a ping.
	 * 
	 * @param config the configuration of the node
	 * @throws IOException if it is not possible to connect to the Tendermint process
	 * @throws TimeoutException if tried many times, but never got a reply
	 * @throws InterruptedException if interrupted while pinging
	 */
	private void waitUntilTendermintProcessIsUp(TendermintBlockchainConfig config) throws TimeoutException, InterruptedException, IOException {
		TendermintPoster poster = new TendermintPoster(config);

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
	
		throw new TimeoutException("Cannot connect to Tendermint process at " + poster.url() + ". Tried " + config.maxPingAttempts + " times");
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
	 * @return the process into which the command is running
	 * @throws IOException if the command cannot be run
	 */
	private Process run(String command, Optional<String> redirection) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder();

		if (isWindows) // Windows is different
			command = "cmd.exe /c " + command;

		processBuilder.command(command.split(" "));
		redirection.ifPresent(where -> processBuilder.redirectOutput(new File(where)));

        return processBuilder.start();
	}
}