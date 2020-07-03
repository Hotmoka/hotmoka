package io.hotmoka.xodus.env;

import java.util.function.Consumer;
import java.util.function.Function;

import io.hotmoka.xodus.ExodusException;
import jetbrains.exodus.env.StoreConfig;

public class Environment {
	private final jetbrains.exodus.env.Environment parent;
	
	public Environment(String dir) {
		this.parent = jetbrains.exodus.env.Environments.newInstance(dir);
	}

	public void close() throws ExodusException {
		try {
			parent.close();
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	public Transaction beginTransaction() {
		return new Transaction(parent.beginTransaction());
	}

	public void executeInTransaction(Consumer<Transaction> executable) {
		parent.executeInTransaction(txn -> executable.accept(new Transaction(txn)));
	}

	public <T> T computeInReadonlyTransaction(Function<Transaction, T> computable) {
		return parent.computeInReadonlyTransaction(txn -> computable.apply(new Transaction(txn)));
	}

	public Store openStoreWithoutDuplicates(String name, Transaction txn) {
		return Store.fromNative(parent.openStore(name, StoreConfig.WITHOUT_DUPLICATES, txn.toNative()));
	}
}