package io.hotmoka.tendermint.internal;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.takamaka.code.engine.AbstractNodeWithHistory;

/**
 * An implementation of a blockchain working over the Tendermint generic blockchain engine.
 * Requests sent to this blockchain are forwarded to a Tendermint process. This process
 * checks and delivers such requests, by calling the ABCI interface. This blockchain keeps
 * its state in a transactional database implemented by the {@linkplain State} class.
 */
public class TendermintBlockchainImpl extends AbstractNodeWithHistory<Config> implements TendermintBlockchain {
	private final static Logger logger = LoggerFactory.getLogger(TendermintBlockchainImpl.class);

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

	/**
	 * Builds a Tendermint blockchain. This constructor spawns the Tendermint process on localhost
	 * and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 */
	public TendermintBlockchainImpl(Config config) {
		super(config);

		try {
			this.state = new State(config.dir + "/state");
			this.abci = ServerBuilder.forPort(config.abciPort).addService(new ABCI(this)).build();
			this.abci.start();
			this.tendermint = new Tendermint(this);
		}
		catch (Exception e) {
			logger.error("failed creating the Tendermint blockchain", e);

			try {
				close();
			}
			catch (Exception e1) {
				logger.error("cannot close the blockchain", e1);
			}

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
	protected StorageReference getManifestUncommitted() throws NoSuchElementException {
		return state.getManifest().orElseThrow(() -> new NoSuchElementException("no manifest set for this node"));
	}

	@Override
	protected long getNow() {
		return now;
	}

	@Override
	protected boolean isInitialized() {
		return state.getManifest().isPresent();
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
			return state.getResponseUncommitted(reference)
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
	protected void expandStore(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		state.expand(this, reference, request, response);
	}

	@Override
	protected void expandStore(TransactionReference reference, TransactionRequest<?> request, String errorMessage) {
		// nothing to do, since Tendermint keeps the error message inside the blockchain, in the field "data" of its transactions
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
	 * Yields the hash of the state.
	 * 
	 * @return the hash
	 */
	byte[] getStateHash() {
		return state.getHash();
	}
}