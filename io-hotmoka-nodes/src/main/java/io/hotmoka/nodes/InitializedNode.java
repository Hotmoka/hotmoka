package io.hotmoka.nodes;

import java.util.Optional;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.values.StorageReference;

/**
 * A node that provides access to some previously installed jars and to
 * a predefined set of accounts and the base Takamaka code installed in the node.
 */
public interface InitializedNode extends Node {

	/**
	 * Yields the reference, in the store of the node, where the base Takamaka base classes have been installed.
	 */
	Classpath takamakaCode();

	/**
	 * Yields the reference, in the store of the node, where the a user jar has been installed, if any.
	 * This jar is typically referred to at construction time of the node.
	 */
	Optional<Classpath> jar();

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in the store of the node. This is a {@link #io.takamaka.code.lang.TestExternallyOwnedAccount}}
	 */
	StorageReference account(int i);
}