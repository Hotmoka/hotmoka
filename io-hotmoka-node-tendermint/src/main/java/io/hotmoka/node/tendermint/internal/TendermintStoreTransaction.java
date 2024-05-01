package io.hotmoka.node.tendermint.internal;

import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.stores.AbstractTrieBasedStoreTransaction;
import io.hotmoka.stores.StoreException;
import io.hotmoka.xodus.env.Transaction;

public class TendermintStoreTransaction extends AbstractTrieBasedStoreTransaction<TendermintStore> {

	protected TendermintStoreTransaction(TendermintStore store, Object lock, Transaction txn) throws StoreException {
		super(store, lock, txn);
	}

	@Override
	protected void setRequest(TransactionReference reference, TransactionRequest<?> request) throws StoreException {
		// nothing to do, since Tendermint keeps requests inside its blockchain
	}

	@Override
	protected void setError(TransactionReference reference, String error) throws StoreException {
		// nothing to do, since Tendermint keeps error messages inside the blockchain, in the field "data" of its transactions
	}
}