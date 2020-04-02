package io.hotmoka.nodes;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.values.StorageReference;

/**
 * A node that provides access to a predefined set of accounts and the base Takamaka code
 * installed in the node.
 */
public interface NodeWithAccounts extends Node {

	/**
	 * Yields the reference, in the store of the node, where the base Takamaka classes have been installed.
	 */
	Classpath takamakaCode();

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in the store of the node. This is a {@link #io.takamaka.code.lang.TestExternallyOwnedAccount}}
	 */
	StorageReference account(int i);
}