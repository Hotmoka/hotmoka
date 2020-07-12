package io.hotmoka.takamaka.internal;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.takamaka.Config;
import io.hotmoka.takamaka.TakamakaBlockchain;
import io.takamaka.code.engine.AbstractNodeWithHistory;

/**
 * An implementation of a blockchain working over the Tendermint generic blockchain engine.
 * Requests sent to this blockchain are forwarded to a Tendermint process. This process
 * checks and delivers such requests, by calling the ABCI interface. This blockchain keeps
 * its state in a transactional database implemented by the {@linkplain State} class.
 */
public class TakamakaBlockchainImpl extends AbstractNodeWithHistory<Config> implements TakamakaBlockchain {
	private final static Logger logger = LoggerFactory.getLogger(TakamakaBlockchainImpl.class);

	/**
	 * The transactional state where blockchain data is persisted.
	 */
	private final State state;

	/**
	 * A proxy to the Takamaka process.
	 */
	private final Takamaka takamaka;

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
	public TakamakaBlockchainImpl(Config config) {
		super(config);

		try {
			this.state = new State(config.dir + "/state");
			this.takamaka = new Takamaka(this);
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

			if (takamaka != null)
				takamaka.close();

			if (state != null)
				state.close();

			closed = true;
		}
	}

	/**
	 * Executes the given requests, in the order of the stream,
	 * assuming the given value as current time. Typically, the
	 * requests are the group of smart contract requests contained in a block
	 * whose timestamp if {@code now}.
	 * 
	 * @param now the time to use for the current time
	 * @param requests the requests to execute, in the order of the stream
	 * @return the hash of the state at the end of the execution of the requests
	 */
	public byte[] execute(byte[] initialStateHash, long now, Stream<byte[]> requests) {
		state.beginTransaction();
		this.now = now;
		requests.forEachOrdered(this::processSingleRequest);
		state.commitTransaction();

		return state.getHash();
	}

	public void checkOut(byte[] stateHash) {
		//TODO
	}

	@Override
	protected long getNow() {
		return now;
	}

	@Override
	protected void postTransaction(TransactionRequest<?> request) {
		try {
			// TODO
			//String response = tendermint.broadcastTxAsync(request);
			//tendermint.checkBroadcastTxResponse(response);
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Executes a request.
	 * 
	 * @param bytes the bytes of the request
	 */
	private void processSingleRequest(byte[] bytes) {
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			TransactionRequest<?> request = TransactionRequest.from(ois);
			checkTransaction(request);
			deliverTransaction(request);
		}
		catch (Throwable t) {
			// checkTransaction()/deliverTransaction() already
			// expand the state with an error if there is an exception
		}
	}

	@Override
	protected io.takamaka.code.engine.Store<?> getStore() {
		// TODO Auto-generated method stub
		return null;
	}
}