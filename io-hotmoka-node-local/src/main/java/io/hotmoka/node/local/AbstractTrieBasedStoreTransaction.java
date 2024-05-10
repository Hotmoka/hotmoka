package io.hotmoka.node.local;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.TrieOfErrors;
import io.hotmoka.node.local.internal.TrieOfHistories;
import io.hotmoka.node.local.internal.TrieOfInfo;
import io.hotmoka.node.local.internal.TrieOfRequests;
import io.hotmoka.node.local.internal.TrieOfResponses;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Transaction;

public abstract class AbstractTrieBasedStoreTransaction<S extends AbstractTrieBasedStore<S, T>, T extends AbstractTrieBasedStoreTransaction<S, T>> extends AbstractStoreTransaction<S, T> {

	/**
	 * The Xodus transaction where the updates get recorded.
	 */
	private final Transaction txn;

	/**
	 * The trie of the responses.
	 */
	private volatile TrieOfResponses trieOfResponses;

	/**
	 * The trie for the miscellaneous information.
	 */
	private volatile TrieOfInfo trieOfInfo;

	/**
     * The trie of the errors.
     */
	private volatile TrieOfErrors trieOfErrors;

	/**
	 * The trie of histories.
	 */
	private volatile TrieOfHistories trieOfHistories;

	/**
     * The trie of the requests.
     */
	private volatile TrieOfRequests trieOfRequests;

	protected AbstractTrieBasedStoreTransaction(S store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now, Transaction txn) throws StoreException {
		super(store, executors, consensus, now);

		this.txn = txn;
		this.trieOfResponses = store.mkTrieOfResponses(txn);
		this.trieOfInfo = store.mkTrieOfInfo(txn);
		this.trieOfErrors = store.mkTrieOfErrors(txn);
		this.trieOfHistories = store.mkTrieOfHistories(txn);
		this.trieOfRequests = store.mkTrieOfRequests(txn);
	}

	@Override
	public TransactionResponse getResponseUncommitted(TransactionReference reference) throws UnknownReferenceException, StoreException {
		try {
			return trieOfResponses.get(reference).orElseThrow(() -> new UnknownReferenceException(reference));
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public Stream<TransactionReference> getHistoryUncommitted(StorageReference object) throws StoreException, UnknownReferenceException {
		try {
			return trieOfHistories.get(object).orElseThrow(() -> new UnknownReferenceException(object));
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public Optional<StorageReference> getManifestUncommitted() throws StoreException {
		try {
			return trieOfInfo.getManifest();
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected void setRequest(TransactionReference reference, TransactionRequest<?> request) throws StoreException {
		try {
			trieOfRequests = trieOfRequests.put(reference, request);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected void setResponse(TransactionReference reference, TransactionResponse response) throws StoreException {
		try {
			trieOfResponses = trieOfResponses.put(reference, response);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected void setError(TransactionReference reference, String error) throws StoreException {
		try {
			trieOfErrors = trieOfErrors.put(reference, error);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) throws StoreException {
		try {
			trieOfHistories = trieOfHistories.put(object, history);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected void setManifest(StorageReference manifest) throws StoreException {
		try {
			trieOfInfo = trieOfInfo.setManifest(manifest);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}			
	}
	
	@Override
	public S commit() throws StoreException {
		try {
			trieOfInfo = trieOfInfo.increaseNumberOfCommits();
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}

		if (!txn.commit())
			throw new StoreException("Cannot commit the Xodus transaction");

		return getStore().mkClone(
			getCheckedSignatures(),
			getClassLoaders(),
			getConfigUncommitted(),
			getGasPriceUncommitted(),
			getInflationUncommitted(),
			Optional.of(trieOfResponses.getRoot()),
			Optional.of(trieOfInfo.getRoot()),
			Optional.of(trieOfErrors.getRoot()),
			Optional.of(trieOfHistories.getRoot()),
			Optional.of(trieOfRequests.getRoot())
		);
	}

	@Override
	public void abort() throws StoreException {
		//if (!txn.isFinished()) {
			// store closed with yet uncommitted transactions: we abort them
			//LOGGER.log(Level.WARNING, "store closed with uncommitted transactions: they are being aborted");

		try {
			txn.abort();
		}
		catch (ExodusException e) {
			throw new StoreException(e);
		}
	}
}