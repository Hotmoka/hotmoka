package io.hotmoka.stores;

import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

public abstract class AbstractTrieBasedStoreTransaction<T extends AbstractTrieBasedStore<T>> extends AbstractStoreTransaction<T> {

	protected AbstractTrieBasedStoreTransaction(T store, Object lock) {
		super(store, lock);
	}

	@Override
	public Optional<TransactionResponse> getResponse(TransactionReference reference) {
		System.out.println("response");
		return store.getResponseUncommitted(reference);
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) throws StoreException {
		System.out.println("history");
		new Exception().printStackTrace();
		return store.getHistoryUncommitted(object);
	}

	@Override
	public Optional<StorageReference> getManifest() throws StoreException {
		System.out.println("manifest");
		return store.getManifestUncommitted();
	}

	@Override
	protected void setRequest(TransactionReference reference, TransactionRequest<?> request) throws StoreException {
		store = store.setRequest(reference, request);
	}

	@Override
	protected void setResponse(TransactionReference reference, TransactionResponse response) throws StoreException {
		store = store.setResponse(reference, response);
	}

	@Override
	protected void setError(TransactionReference reference, String error) throws StoreException {
		store = store.setError(reference, error);
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		store = store.setHistory(object, history);
	}

	@Override
	protected void setManifest(StorageReference manifest) throws StoreException {
		store = store.setManifest(manifest);
	}
	
	@Override
	public T commit() {
		return store;
	}
}