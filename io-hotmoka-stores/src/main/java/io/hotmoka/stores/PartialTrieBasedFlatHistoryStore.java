package io.hotmoka.stores;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.xodus.ByteIterable;
import io.takamaka.code.engine.AbstractNode;

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions
 * but not their requests nor errors (for this reason it is <i>partial</i>).
 * It keeps histories in a <i>flat</i> way, that is, it is not possible
 * to come back to a previous history after it has been updated. Hence, this
 * store has no the ability of changing its <i>world view</i> by checking out different
 * hashes of its roots. However, it has a compact representation of histories.
 * Hence, it is useful when coming back in time is not relevant for a node.
 * Responses are kept in a Merkle-Patricia trie, supported by JetBrains' Xodus transactional database.
 * 
 * The information kept in this store consists of:
 * 
 * <ul>
 * <li> a trie that maps each Hotmoka request reference to the response computed for that request
 * <li> a map (non-trie) from each storage reference to the transaction references that contribute
 *      to provide values to the fields of the storage object at that reference (its <i>history</i>);
 *      this is used by a node to reconstruct the state of the objects in store
 * <li> miscellaneous control information, such as where the node's manifest
 *      is installed or the current number of commits
 * </ul>
 * 
 * This information is added in store by push methods and accessed through get methods.
 * 
 * This class is meant to be subclassed by specifying where errors and requests are kept.
 */
@ThreadSafe
public abstract class PartialTrieBasedFlatHistoryStore<N extends AbstractNode<?,?>> extends PartialTrieBasedStore<N> {

	/**
	 * The Xodus store that holds the history of each storage reference, ie, a list of
	 * transaction references that contribute
	 * to provide values to the fields of the storage object at that reference.
	 */
	private final io.hotmoka.xodus.env.Store storeOfHistory;

    /**
	 * Creates a store. Its roots are not yet initialized. Hence, after this constructor,
	 * a call to {@link #setRootsTo(byte[])} or {@link #setRootsAsCheckedOut()}
	 * should occur, to set the roots of the store.
	 * 
	 * @param node the node for which the store is being built
	 */
    protected PartialTrieBasedFlatHistoryStore(N node) {
    	super(node);

    	AtomicReference<io.hotmoka.xodus.env.Store> storeOfHistory = new AtomicReference<>();

    	recordTime(() -> env.executeInTransaction(txn -> {
    		storeOfHistory.set(env.openStoreWithoutDuplicates("history", txn));
    	}));

    	this.storeOfHistory = storeOfHistory.get();
    }

    /**
	 * Builds a clone of the given store.
	 * 
	 * @param parent the store to clone
	 */
	protected PartialTrieBasedFlatHistoryStore(PartialTrieBasedFlatHistoryStore<N> parent) {
		super(parent);

		this.storeOfHistory = parent.storeOfHistory;
	}

	@Override
	public synchronized Stream<TransactionReference> getHistory(StorageReference object) {
		return recordTime(() -> {
			ByteIterable historyAsByteArray = env.computeInReadonlyTransaction(txn -> storeOfHistory.get(txn, intoByteArray(object)));
			return historyAsByteArray == null ? Stream.empty() : Stream.of(fromByteArray(TransactionReference::from, TransactionReference[]::new, historyAsByteArray));
		});
	}

	@Override
	public synchronized Stream<TransactionReference> getHistoryUncommitted(StorageReference object) {
		if (duringTransaction()) {
			ByteIterable historyAsByteArray = storeOfHistory.get(getCurrentTransaction(), intoByteArray(object));
			return historyAsByteArray == null ? Stream.empty() : Stream.of(fromByteArray(TransactionReference::from, TransactionReference[]::new, historyAsByteArray));
		}
		else
			return getHistory(object);
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		recordTime(() -> {
			ByteIterable historyAsByteArray = intoByteArray(history.toArray(TransactionReference[]::new));
			ByteIterable objectAsByteArray = intoByteArray(object);
			storeOfHistory.put(getCurrentTransaction(), objectAsByteArray, historyAsByteArray);
		});
	}

	/**
	 * Commits the current transaction and checks it out, so that it becomes
	 * the current view of the world of this store.
	 */
	public synchronized final void commitTransactionAndCheckout() {
		checkout(commitTransaction());
	}
}