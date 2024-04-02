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

package io.hotmoka.xodus.env;

import java.util.function.Consumer;
import java.util.function.Function;

import io.hotmoka.xodus.ExodusException;
import jetbrains.exodus.env.StoreConfig;

/**
 * An adaptor of a Xodus environment.
 */
public class Environment {
	private final jetbrains.exodus.env.Environment parent;

	/**
	 * Creates a Xodus environment at the given directory.
	 * 
	 * @param dir the directory
	 */
	public Environment(String dir) {
		this.parent = jetbrains.exodus.env.Environments.newInstance(dir);
	}

	/**
	 * Creates a Xodus environment at the given directory with thegiven configuration.
	 * 
	 * @param dir the directory
	 * @param config the configuration
	 */
	public Environment(String dir, EnvironmentConfig config) {
		this.parent = jetbrains.exodus.env.Environments.newInstance(dir, config.toNative());
	}

	/**
	 * Closes this environment.
	 * 
	 * @throws ExodusException if closure fals
	 */
	public void close() throws ExodusException {
		try {
			parent.close();
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	/**
	 * Starts a new transaction.
	 * 
	 * @return the new transaction
	 * @throws ExodusException if the operation fails
	 */
	public Transaction beginTransaction() throws ExodusException {
		try {
			return new Transaction(parent.beginTransaction());
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	/**
     * Executes the specified executable in a new transaction. If the transaction cannot be flushed
     * at its end, the executable is executed once more until the transaction is finally flushed.
     *
     * @param executable the transactional executable
     * @throws ExodusException if the operation fails
     */
	public void executeInTransaction(Consumer<Transaction> executable) throws ExodusException {
		try {
			parent.executeInTransaction(txn -> executable.accept(new Transaction(txn)));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	 /**
     * Executes the specified executable in a new read-only transaction, only once,
     * since the transaction is read-only and is never flushed.
     *
     * @param executable transactional executable
     * @throws ExodusException if the operation fails
     */
	public void executeInReadonlyTransaction(Consumer<Transaction> executable) throws ExodusException {
		try {
			parent.executeInReadonlyTransaction(txn -> executable.accept(new Transaction(txn)));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

    /**
     * Computes and returns a value by calling the specified computable in a new read-only transaction,
     * only once, since the transaction is read-only and is never flushed.
     *
     * @param <T> the type of the value returned by {@code computable}
     * @param computable the transactional computable
     * @return the result of the computable
     * @throws ExodusException if the operation fails
     */
	public <T> T computeInReadonlyTransaction(Function<Transaction, T> computable) throws ExodusException {
		try {
			return parent.computeInReadonlyTransaction(txn -> computable.apply(new Transaction(txn)));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

    /**
     * Computes and returns a value by calling the specified computable in a new transaction.
     * If the transaction cannot be flushed
     * at its end, the executable is executed once more until the transaction is finally flushed.
     *
     * @param <T> the type of the value returned by {@code computable}
     * @param computable the transactional computable
     * @return the result of the computable
     * @throws ExodusException if the operation fails
     */
	public <T> T computeInTransaction(Function<Transaction, T> computable) throws ExodusException {
		try {
			return parent.computeInTransaction(txn -> computable.apply(new Transaction(txn)));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

    /**
     * Opens an existing or creates a new store with the specified name, inside the given transaction.
     *
     * @param name the name of the store
     * @param txn the transaction used to create store
     * @return the resulting store instance
     * @throws ExodusException if the operation fails
     */
	public Store openStoreWithoutDuplicates(String name, Transaction txn) throws ExodusException {
		try {
			return new Store(parent.openStore(name, StoreConfig.WITHOUT_DUPLICATES, txn.toNative()));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}
}