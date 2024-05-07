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

package io.hotmoka.node.local;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.StoreTransaction;
import io.hotmoka.node.local.internal.KeyValueStoreOnXodus;
import io.hotmoka.node.local.internal.TrieOfErrors;
import io.hotmoka.node.local.internal.TrieOfHistories;
import io.hotmoka.node.local.internal.TrieOfInfo;
import io.hotmoka.node.local.internal.TrieOfRequests;
import io.hotmoka.node.local.internal.TrieOfResponses;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Transaction;

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions
 * but not their requests, histories and errors (for this reason it is <i>partial</i>).
 * Its implementation is based on Merkle-Patricia tries,
 * supported by JetBrains' Xodus transactional database.
 * 
 * The information kept in this store consists of:
 * 
 * <ul>
 * <li> a map from each Hotmoka request reference to the response computed for that request
 * <li> miscellaneous control information, such as where the node's manifest
 *      is installed or the current root and number of commits
 * </ul>
 * 
 * This information is added in store by push methods and accessed through get methods.
 * 
 * This class is meant to be subclassed by specifying where errors, requests and histories are kept.
 */
@Immutable
public abstract class AbstractTrieBasedStore<S extends AbstractTrieBasedStore<S, N>, N extends AbstractLocalNode<N, ?, S>> extends AbstractStore<S, N> {

	/**
	 * The Xodus environment that holds the store.
	 */
	private final Environment env;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the responses to the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfResponses;

	/**
	 * The Xodus store that holds miscellaneous information about the store.
	 */
    private final io.hotmoka.xodus.env.Store storeOfInfo;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the errors of the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfErrors;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfRequests;

	/**
	 * The Xodus store that holds the history of each storage reference, ie, a list of
	 * transaction references that contribute
	 * to provide values to the fields of the storage object at that reference.
	 */
	private final io.hotmoka.xodus.env.Store storeOfHistories;

	/**
	 * The root of the trie of the responses. It is empty if the trie is empty.
	 */
	private final Optional<byte[]> rootOfResponses;

	/**
	 * The root of the trie of the miscellaneous info. It is empty if the trie is empty.
	 */
	private final Optional<byte[]> rootOfInfo;

	/**
	 * The root of the trie of the errors. It is empty if the trie is empty.
	 */
	private final Optional<byte[]> rootOfErrors;

	/**
	 * The root of the trie of the requests. It is empty if the trie is empty.
	 */
	private final Optional<byte[]> rootOfRequests;

	/**
	 * The root of the trie of histories. It is empty if the trie is empty.
	 */
	private final Optional<byte[]> rootOfHistories;

	/**
	 * The key used inside {@link #storeOfInfo} to keep the root.
	 */
	private final static ByteIterable ROOT = ByteIterable.fromBytes("root".getBytes());

	/**
	 * Creates a store. Its roots are initialized as in the Xodus store, if present.
	 * 
 	 * @param dir the path where the database of the store is kept
	 */
    protected AbstractTrieBasedStore(N node, ConsensusConfig<?,?> consensus) {
    	super(node, consensus);

    	this.env = new Environment(node.getLocalNodeConfig().getDir() + "/store");

		var storeOfInfo = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var roots = new AtomicReference<Optional<byte[]>>();

		env.executeInTransaction(txn -> {
			storeOfInfo.set(env.openStoreWithoutDuplicates("info", txn));
    		roots.set(Optional.ofNullable(storeOfInfo.get().get(txn, ROOT)).map(ByteIterable::getBytes));
    	});

    	var storeOfResponses = new AtomicReference<io.hotmoka.xodus.env.Store>();
    	var storeOfErrors = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var storeOfRequests = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var storeOfHistories = new AtomicReference<io.hotmoka.xodus.env.Store>();

		env.executeInTransaction(txn -> {
			storeOfResponses.set(env.openStoreWithoutDuplicates("responses", txn));
			storeOfErrors.set(env.openStoreWithoutDuplicates("errors", txn));
			storeOfRequests.set(env.openStoreWithoutDuplicates("requests", txn));
			storeOfHistories.set(env.openStoreWithoutDuplicates("history", txn));
		});

    	this.storeOfResponses = storeOfResponses.get();
    	this.storeOfInfo = storeOfInfo.get();
    	this.storeOfErrors = storeOfErrors.get();
		this.storeOfRequests = storeOfRequests.get();
		this.storeOfHistories = storeOfHistories.get();

    	Optional<byte[]> hashesOfRoots = roots.get();

    	if (hashesOfRoots.isEmpty()) {
    		rootOfResponses = Optional.empty();
    		rootOfInfo = Optional.empty();
    		rootOfErrors = Optional.empty();
    		rootOfRequests = Optional.empty();
    		rootOfHistories = Optional.empty();
    	}
    	else {
    		var rootOfResponses = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 0, rootOfResponses, 0, 32);
    		this.rootOfResponses = Optional.of(rootOfResponses);

    		var rootOfInfo = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 32, rootOfInfo, 0, 32);
    		this.rootOfInfo = Optional.of(rootOfInfo);

    		var rootOfErrors = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 64, rootOfErrors, 0, 32);
    		this.rootOfErrors = Optional.of(rootOfErrors);

    		var rootOfRequests = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 96, rootOfRequests, 0, 32);
    		this.rootOfRequests = Optional.of(rootOfRequests);

    		var rootOfHistory = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 128, rootOfHistory, 0, 32);
    		this.rootOfHistories = Optional.of(rootOfHistory);
    	}
    }

    protected AbstractTrieBasedStore(AbstractTrieBasedStore<S, N> toClone) {
    	super(toClone);

    	this.env = toClone.env;
    	this.storeOfResponses = toClone.storeOfResponses;
    	this.storeOfInfo = toClone.storeOfInfo;
    	this.storeOfErrors = toClone.storeOfErrors;
    	this.storeOfHistories = toClone.storeOfHistories;
    	this.storeOfRequests = toClone.storeOfRequests;
    	this.rootOfResponses = toClone.rootOfResponses;
    	this.rootOfInfo = toClone.rootOfInfo;
    	this.rootOfErrors = toClone.rootOfErrors;
    	this.rootOfHistories = toClone.rootOfHistories;
    	this.rootOfRequests = toClone.rootOfRequests;
    }

    protected AbstractTrieBasedStore(AbstractTrieBasedStore<S, N> toClone, ConsensusConfig<?,?> consensus, Optional<BigInteger> gasPrice, OptionalLong inflation, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfErrors, Optional<byte[]> rootOfHistories, Optional<byte[]> rootOfRequests) {
    	super(toClone, consensus, gasPrice, inflation);

    	this.env = toClone.env;
    	this.storeOfResponses = toClone.storeOfResponses;
    	this.storeOfInfo = toClone.storeOfInfo;
    	this.storeOfErrors = toClone.storeOfErrors;
    	this.storeOfHistories = toClone.storeOfHistories;
    	this.storeOfRequests = toClone.storeOfRequests;
    	this.rootOfResponses = rootOfResponses;
    	this.rootOfInfo = rootOfInfo;
    	this.rootOfErrors = rootOfErrors;
    	this.rootOfHistories = rootOfHistories;
    	this.rootOfRequests = rootOfRequests;
    }

    protected abstract S mkClone(ConsensusConfig<?,?> consensus, Optional<BigInteger> gasPrice, OptionalLong inflation, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfErrors, Optional<byte[]> rootOfHistories, Optional<byte[]> rootOfRequests);

    @Override
    public void close() throws StoreException {
    	try {
    		env.close();
    	}
    	catch (ExodusException e) {
    		throw new StoreException(e);
    	}
    }

    @Override
    public Optional<TransactionResponse> getResponse(TransactionReference reference) {
    	return env.computeInReadonlyTransaction // TODO: recheck
    		(UncheckFunction.uncheck(txn -> mkTrieOfResponses(txn).get(reference)));
	}

	@Override
	public Optional<StorageReference> getManifest() throws StoreException {
		try {
			return CheckSupplier.check(TrieException.class, () -> env.computeInReadonlyTransaction
				(UncheckFunction.uncheck(txn -> mkTrieOfInfo(txn).getManifest())));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public Optional<String> getError(TransactionReference reference) throws StoreException {
		try {
			return CheckSupplier.check(TrieException.class, () -> env.computeInReadonlyTransaction
					(UncheckFunction.uncheck(txn -> mkTrieOfErrors(txn).get(reference))));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		return env.computeInReadonlyTransaction // TODO: recheck
			(UncheckFunction.uncheck(txn -> mkTrieOfRequests(txn).get(reference)));
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) throws StoreException {
		try {
			return CheckSupplier.check(TrieException.class, () -> env.computeInReadonlyTransaction
				(UncheckFunction.uncheck(txn -> mkTrieOfHistories(txn).get(object))).orElse(Stream.empty()));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public StoreTransaction<S> beginTransaction(long now) throws StoreException {
		return mkTransaction(env.beginTransaction(), now);
	}

	/**
	 * Yields the number of commits already performed over this store.
	 * 
	 * @return the number of commits
	 */
	public long getNumberOfCommits() {
		return env.computeInReadonlyTransaction // TODO: recheck
			(UncheckFunction.uncheck(txn -> mkTrieOfInfo(txn).getNumberOfCommits()));
	}

	public byte[] getStateId() throws StoreException {
		return mergeRootsOfTries();
	}

	/**
	 * Resets the store to the given root. This is just the concatenation of the roots
	 * of the tries in this store. For instance, as returned by a previous {@link #commitTransaction()}.
	 * 
	 * @param root the root to reset to
	 */
	public S checkoutAt(byte[] root) {
		var bytesOfRootOfResponses = new byte[32];
		System.arraycopy(root, 0, bytesOfRootOfResponses, 0, 32);
		var bytesOfRootOfInfo = new byte[32];
		System.arraycopy(root, 32, bytesOfRootOfInfo, 0, 32);
		var bytesOfRootOfErrors = new byte[32];
		System.arraycopy(root, 64, bytesOfRootOfErrors, 0, 32);
		var bytesOfRootOfRequests = new byte[32];
		System.arraycopy(root, 96, bytesOfRootOfRequests, 0, 32);
		var bytesOfRootOfHistories = new byte[32];
		System.arraycopy(root, 128, bytesOfRootOfHistories, 0, 32);

		try {
			S temp = mkClone(ValidatorsConsensusConfigBuilders.defaults().build(), Optional.empty(), OptionalLong.empty(), Optional.of(bytesOfRootOfResponses), Optional.of(bytesOfRootOfInfo), Optional.of(bytesOfRootOfErrors), Optional.of(bytesOfRootOfHistories), Optional.of(bytesOfRootOfRequests));
			var storeTransaction = temp.beginTransaction(System.currentTimeMillis());
			storeTransaction.invalidateConsensusCache();
			ConsensusConfig<?,?> consensus = storeTransaction.getConfigUncommitted();
			storeTransaction.abort();
			return mkClone(consensus, Optional.empty(), OptionalLong.empty(), Optional.of(bytesOfRootOfResponses), Optional.of(bytesOfRootOfInfo), Optional.of(bytesOfRootOfErrors), Optional.of(bytesOfRootOfHistories), Optional.of(bytesOfRootOfRequests));
		}
		catch (NoSuchAlgorithmException | StoreException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	public void moveRootBranchToThis() throws StoreException {
		var rootAsBI = ByteIterable.fromBytes(mergeRootsOfTries());
		env.executeInTransaction(txn -> storeOfInfo.put(txn, ROOT, rootAsBI));
	}

	protected abstract StoreTransaction<S> mkTransaction(Transaction txn, long now) throws StoreException;

	protected TrieOfResponses mkTrieOfResponses(Transaction txn) throws StoreException {
		try {
			return new TrieOfResponses(new KeyValueStoreOnXodus(storeOfResponses, txn), rootOfResponses);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	protected TrieOfInfo mkTrieOfInfo(Transaction txn) throws StoreException {
		try {
			return new TrieOfInfo(new KeyValueStoreOnXodus(storeOfInfo, txn), rootOfInfo);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	protected TrieOfErrors mkTrieOfErrors(Transaction txn) throws StoreException {
		try {
			return new TrieOfErrors(new KeyValueStoreOnXodus(storeOfErrors, txn), rootOfErrors);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	protected TrieOfRequests mkTrieOfRequests(Transaction txn) throws StoreException {
		try {
			return new TrieOfRequests(new KeyValueStoreOnXodus(storeOfRequests, txn), rootOfRequests);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	protected TrieOfHistories mkTrieOfHistories(Transaction txn) throws StoreException {
		try {
			return new TrieOfHistories(new KeyValueStoreOnXodus(storeOfHistories, txn), rootOfHistories);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Yields the concatenation of the roots of the tries in this store,
	 * resulting after all updates performed to the store. Hence, they point
	 * to the latest view of the store.
	 * 
	 * @return the concatenation
	 */
	private byte[] mergeRootsOfTries() throws StoreException {
		var result = new byte[160];
		System.arraycopy(rootOfResponses.get(), 0, result, 0, 32);
		System.arraycopy(rootOfInfo.get(), 0, result, 32, 32);
		System.arraycopy(rootOfErrors.get(), 0, result, 64, 32);
		System.arraycopy(rootOfRequests.get(), 0, result, 96, 32);
		System.arraycopy(rootOfHistories.get(), 0, result, 128, 32);

		return result;
	}

	/**
	 * Determines if all roots of the tries in this store are empty.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected boolean isEmpty() {
		return rootOfResponses.isEmpty() && rootOfInfo.isEmpty() && rootOfErrors.isEmpty() && rootOfRequests.isEmpty() && rootOfHistories.isEmpty();
	}
}