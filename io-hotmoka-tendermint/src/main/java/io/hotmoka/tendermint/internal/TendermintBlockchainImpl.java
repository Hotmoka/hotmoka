package io.hotmoka.tendermint.internal;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.Initialization;

/**
 * An implementation of a blockchain working over the Tendermint generic blockchain engine.
 * Requests sent to this blockchain are forwarded to a Tendermint process. This process
 * checks and delivers such requests, by calling the ABCI interface. The Tendermint response contains
 * the Hotmoka transaction reference that has been selected for each request that gets executed,
 * or an error message. This blockchain keeps its state in a transactional database
 * implemented by the {@linkplain State} class.
 */
public class TendermintBlockchainImpl extends AbstractNode<Config>implements TendermintBlockchain {

	/**
	 * The GRPC server that runs the ABCI process.
	 */
	private final Server abci;

	/**
	 * A proxy to the Tendermint process.
	 */
	private final Tendermint tendermint;

	/**
	 * The transactional state where blockchain data is persisted.
	 */
	private final State state;

	/**
	 * The reference, in the blockchain, where the base Takamaka classes have been installed.
	 * This is copy of information in the state, for efficiency.
	 */
	private final Classpath takamakaCode;

	/**
	 * The classpath of a user jar that has been installed, if any.
	 * This is mainly used to simplify the tests.
	 * This is copy of information in the state, for efficiency.
	 */
	private final Classpath jar;

	/**
	 * The accounts created during initialization.
	 * This is copy of information in the state, for efficiency.
	 */
	private final StorageReference[] accounts;

	/**
	 * True if this blockchain has been already closed. Used to avoid double-closing in the shutdown hook.
	 */
	private volatile boolean closed;

	/**
	 * The current time of the blockchain, set when each block gets created.
	 */
	private volatile long now;

	private final static Logger logger = LoggerFactory.getLogger(TendermintBlockchainImpl.class);

	/**
	 * Builds a Tendermint blockchain, install the basic jar in it
	 * and initializes user accounts with the given initial funds.
	 * This constructor spawns the Tendermint process on localhost and connects it to an ABCI application
	 * for handling its transactions. Blockchain data gets deleted if it already existed.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@linkplain #takamakaCode()}
	 * @param jar the path of a jar that must be further installed in blockchain. This is optional and mainly
	 *            useful to simplify the implementation of tests
	 * @param redGreen true if red/green accounts must be created; if false, normal accounts are created
	 * @param funds the initial funds of the accounts that are created; if {@code redGreen} is true,
	 *              they must be understood in pairs, each pair for the green/red initial funds of each account (green before red)
	 * @throws Exception if the blockchain could not be created
	 */
	public TendermintBlockchainImpl(Config config, Path takamakaCodePath, Optional<Path> jar, boolean redGreen, BigInteger... funds) throws Exception {
		super(config);

		try {
			deleteDir(config.dir);
			this.state = new State(config.dir + "/state");
			this.abci = ServerBuilder.forPort(config.abciPort).addService(new ABCI(this)).build();
			this.abci.start();
			this.tendermint = new Tendermint(this, true);
			addShutdownHook();
			Initialization init = new Initialization(this, takamakaCodePath, jar.isPresent() ? jar.get() : null, redGreen, funds);
			this.jar = init.jar;
			this.accounts = init.accounts().toArray(StorageReference[]::new);
			this.takamakaCode = init.takamakaCode;
			state.putTakamakaCode(takamakaCode);
			Stream.of(accounts).forEach(state::putAccount);
			jar.ifPresent(__ -> state.putJar(this.jar));
		}
		catch (Throwable t) {
			deleteDir(config.dir); // do not leave zombies behind
			close();
			throw t;
		}
	}

	/**
	 * Builds a Tendermint blockchain and initializes it with the information already
	 * existing at its configuration directory. This constructor can be used to
	 * recover a blockchain already created in the past, with all its information.
	 * A Tendermint blockchain must have been already successfully created at
	 * its configuration directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @throws Exception if the blockchain could not be created
	 */
	public TendermintBlockchainImpl(Config config) throws Exception {
		super(config);

		try {
			this.state = new State(config.dir + "/state");
			this.abci = ServerBuilder.forPort(config.abciPort).addService(new ABCI(this)).build();
			this.abci.start();
			this.tendermint = new Tendermint(this, false);
			addShutdownHook();
			this.jar = state.getJar().orElse(null);
			this.accounts = state.getAccounts().toArray(StorageReference[]::new);
			this.takamakaCode = state.getTakamakaCode().get();
		}
		catch (Throwable t) {
			close();
			throw t;
		}
	}

	@Override
	public void close() throws Exception {
		if (!closed) { // avoid double close
			super.close();

			if (tendermint != null)
				tendermint.close();

			if (abci != null && !abci.isShutdown()) {
				abci.shutdown();
				abci.awaitTermination();
			}

			if (state != null)
				state.close();

			closed = true;
		}
	}

	@Override
	public StorageReference account(int i) {
		return accounts[i];
	}

	@Override
	public Classpath takamakaCode() {
		return takamakaCode;
	}

	@Override
	public Optional<Classpath> jar() {
		return Optional.ofNullable(jar);
	}

	@Override
	public long getNow() {
		return now;
	}

	@Override
	protected TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		try {
			tendermint.getErrorMessage(reference.getHash())
				.ifPresent(TransactionRejectedException::new);

			return state.getResponse(reference)
				.orElseThrow(() -> new NoSuchElementException("unknown transaction reference " + reference));
		}
		catch (TransactionRejectedException | NoSuchElementException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void postTransaction(TransactionRequest<?> request) {
		try {
			String response = tendermint.broadcastTxAsync(request);
			tendermint.checkBroadcastTxResponse(response);
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	protected Stream<TransactionReference> getHistory(StorageReference object) {
		return state.getHistory(object);
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		state.putHistory(object, history);
	}

	@Override
	protected void expandStore(TransactionRequest<?> request, TransactionResponse response) {
		state.putResponse(referenceOf(request), response);
		super.expandStore(request, response);
	}

	/**
	 * Yields the number of commits already performed with this blockchain.
	 * 
	 * @return the number of commits
	 */
	long getNumberOfCommits() {
		return state.getNumberOfCommits();
	}

	/**
	 * Starts a new block, at the given time.
	 * This is called by the ABCI when it needs to create a new block.
	 * 
	 * @param now the time when the block is being created
	 */
	void beginBlock(long now) {
		state.beginTransaction();
		this.now = now;
	}

	/**
	 * Commits the current block.
	 * This is called by the ABCI when it needs to commit the current block.
	 */
	void commitBlock() {
		state.commitTransaction();
	}

	/**
	 * Deletes the given directory, recursively.
	 * 
	 * @param dir the directory to delete
	 * @throws IOException if the directory or some of its subdirectories cannot be deleted
	 */
	private static void deleteDir(Path dir) throws IOException {
		if (Files.exists(dir))
			Files.walk(dir)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}

	/**
	 * Adds a shutdown hook that shuts down the blockchain orderly if the JVM terminates.
	 */
	private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				close();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
	}
}