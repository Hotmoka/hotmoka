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

/**
 * A transaction is an access scope to data in database. Any transaction holds a database snapshot,
 * thus providing <a href="https://en.wikipedia.org/wiki/Snapshot_isolation">snapshot isolation</a>.
 * All changes made in a transaction are atomic and consistent if they are successfully flushed or committed.
 */
public class Transaction {
	private final jetbrains.exodus.env.Transaction parent;

	/**
	 * Creates a new transaction that adapts the given Xodus transaction.
	 * 
	 * @param parent the transaction to adapt
	 */
	Transaction(jetbrains.exodus.env.Transaction parent) {
		this.parent = parent;
	}

	/**
	 * Yields the Xodus transaction adapted by this object.
	 * 
	 * @return the Xodus transaction adapted by this object
	 */
	public jetbrains.exodus.env.Transaction toNative() {
		return parent;
	}

	/**
	 * Determines if this transaction is finished.
	 * 
	 * @return true if and only this transaction is finished
	 */
	public boolean isFinished() {
		return parent.isFinished();
	}

	/**
	 * Aborts this transaction.
	 */
	public void abort(){
		parent.abort();
	}

	/**
	 * Commits this transaction.
	 * 
	 * @return true if and only if the transaction has been actually committed
	 */
	public boolean commit() {
		return parent.commit();
	}
}