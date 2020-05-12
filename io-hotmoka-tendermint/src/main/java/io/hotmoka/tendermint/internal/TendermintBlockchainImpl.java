package io.hotmoka.tendermint.internal;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.takamaka.code.engine.AbstractNode;

/**
 * An implementation of a blockchain working over the Tendermint generic blockchain engine.
 * Requests sent to this blockchain are forwarded to a Tendermint process. This process
 * checks and delivers such requests, by calling the ABCI interface. This blockchain keeps
 * its state in a transactional database implemented by the {@linkplain State} class.
 */
public class TendermintBlockchainImpl extends AbstractNode<Config> implements TendermintBlockchain {

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
	 * True if this blockchain has been already closed. Used to avoid double-closing in the shutdown hook.
	 */
	private volatile boolean closed;

	/**
	 * The current time of the blockchain, set when each block gets created.
	 */
	private volatile long now;

	private final static Logger logger = LoggerFactory.getLogger(TendermintBlockchainImpl.class);

	/**
	 * Builds a Tendermint blockchain and installs a jar in it.
	 * This constructor spawns the Tendermint process on localhost and connects it to an ABCI application
	 * for handling its transactions. Blockchain data gets deleted if it already existed.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCode the path where the base Takamaka classes can be found. If present, it will be
	 *                     installed in blockchain and will be available later as {@linkplain #takamakaCode()}.
	 *                     If absent, the previous jar installed to that purpose will be used, if the
	 *                     directory of the blockchain has not been deleted
	 * @throws TransactionRejectedException if some initialization transaction failed
	 */
	public TendermintBlockchainImpl(Config config, Optional<Path> takamakaCode) throws TransactionRejectedException {
		super(config);

		try {
			this.state = new State(config.dir + "/state");
			this.abci = ServerBuilder.forPort(config.abciPort).addService(new ABCI(this)).build();
			this.abci.start();
			this.tendermint = new Tendermint(this);

			if (takamakaCode.isPresent())
				completeCreation(() -> installJar(takamakaCode.get()));
			else
				completeCreation(() -> state.getTakamakaCode().orElse(null));
		}
		catch (Exception e) {
			logger.error("failed creating the Tendermint blockchain", e);

			try {
				close();
			}
			catch (Exception e1) {
				logger.error("cannot close the blockchain", e1);
			}

			if (e instanceof TransactionRejectedException)
				throw (TransactionRejectedException) e;
			else
				throw InternalFailureException.of(e);
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
	public long getNow() {
		return now;
	}

	@Override
	protected boolean isCommitted(TransactionReference reference) {
		try {
			return tendermint.getRequest(reference.getHash()).isPresent();
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	protected TransactionRequest<?> getRequest(TransactionReference reference) {
		try {
			return tendermint.getRequest(reference.getHash()).get();
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	protected TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException {
		try {
			Optional<String> error = tendermint.getErrorMessage(reference.getHash());
			if (error.isPresent())
				throw new TransactionRejectedException(error.get());
			else
				return state.getResponse(reference)
					.orElseThrow(() -> new InternalFailureException("transaction reference " + reference + " is committed but the state has no information about it"));
		}
		catch (TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	protected TransactionResponse getResponseUncommitted(TransactionReference reference) {
		try {
			return state.getResponse(reference)
				.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + reference));
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
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
	protected void expandStore(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		state.putResponse(reference, response);
		super.expandStore(reference, request, response);

		if (response instanceof JarStoreInitialTransactionResponse && ((JarStoreInitialTransactionRequest) request).setAsTakamakaCode)
			state.putTakamakaCode(takamakaCode()); // for future recreation
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
}