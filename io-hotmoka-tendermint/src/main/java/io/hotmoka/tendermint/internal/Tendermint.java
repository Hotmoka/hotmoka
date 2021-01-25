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
import java.net.MalformedURLException;
import java.net.URL;
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

		if (System.getProperty("os.name").startsWith("Windows"))
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
			//if (run("tendermint testnet --v 1 --o " + tendermintHome + " --populate-persistent-peers", Optional.empty()).waitFor() != 0)
			if (run("tendermint init --home " + tendermintHome, Optional.empty()).waitFor() != 0)
				throw new IOException("Tendermint initialization failed");
		}
		else
			// we clone the configuration files inside node.config.tendermintConfigurationToClone
			// into the directory tendermintHome
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
		//this.process = run("tendermint node --home " + tendermintHome + "/node0 --abci grpc --proxy_app tcp://127.0.0.1:" + node.config.abciPort, Optional.of("tendermint.log"));
		return run("tendermint node --home " + tendermintHome + " --abci grpc --proxy_app tcp://127.0.0.1:" + config.abciPort, Optional.of("tendermint.log"));
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
		for (int reconnections = 1; reconnections <= config.maxPingAttempts; reconnections++) {
			try {
				HttpURLConnection connection = openPostConnectionToTendermint(config);
				try (OutputStream os = connection.getOutputStream(); InputStream is = connection.getInputStream()) {
					return;
				}
			}
			catch (ConnectException e) {
				// take a nap, then try again
				Thread.sleep(config.pingDelay);
			}
		}
	
		throw new TimeoutException("Cannot connect to Tendermint process at " + url(config) + ". Tried " + config.maxPingAttempts + " times");
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
	private static Process run(String command, Optional<String> redirection) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder();

		if (System.getProperty("os.name").startsWith("Windows")) // Windows is different
			command = "cmd.exe /c " + command;

		processBuilder.command(command.split(" "));

        if (redirection.isPresent())
        	processBuilder.redirectOutput(new File(redirection.get()));

        return processBuilder.start();
	}

	/**
	 * Yields the URL of the Tendermint process.
	 * 
	 * @param config the configuration of the node
	 * @return the URL
	 * @throws MalformedURLException if the URL is not well formed
	 */
	private URL url(TendermintBlockchainConfig config) throws MalformedURLException {
		return new URL("http://127.0.0.1:" + config.tendermintPort);
	}

	/**
	 * Opens a http POST connection to the Tendermint process.
	 * 
	 * @param config the configuration of the node
	 * @return the connection
	 * @throws IOException if the connection cannot be opened
	 */
	private HttpURLConnection openPostConnectionToTendermint(TendermintBlockchainConfig config) throws IOException {
		HttpURLConnection con = (HttpURLConnection) url(config).openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);

		return con;
	}
}