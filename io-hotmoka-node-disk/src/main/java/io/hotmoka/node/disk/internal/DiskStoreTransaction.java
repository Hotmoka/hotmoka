package io.hotmoka.node.disk.internal;

import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.stores.AbstractStoreTransaction;
import io.hotmoka.stores.StoreException;

public class DiskStoreTransaction extends AbstractStoreTransaction<DiskStore> {

	public DiskStoreTransaction(DiskStore store, Object lock) {
		super(store, lock);
	}

	@Override
	public Optional<TransactionResponse> getResponse(TransactionReference reference) {
		return store.getResponseUncommitted(reference);
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) throws StoreException {
		return store.getHistoryUncommitted(object);
	}

	@Override
	public Optional<StorageReference> getManifest() throws StoreException {
		return store.getManifestUncommitted();
	}

	@Override
	public DiskStore commit() {
		return store;
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
}