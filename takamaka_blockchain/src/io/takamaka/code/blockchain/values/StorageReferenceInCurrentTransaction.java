package io.takamaka.code.blockchain.values;

import java.math.BigInteger;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.DeserializationError;
import io.takamaka.code.blockchain.TransactionReference;
import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.runtime.AbstractStorage;

/**
 * A reference to an object of class type that is not yet stored in the blockchain,
 * since it has been created during the current transaction.
 * Objects created during the same transaction are disambiguated by a progressive number.
 */
@Immutable
public final class StorageReferenceInCurrentTransaction extends StorageReference {

	private static final long serialVersionUID = -9199432347895285763L;

	/**
	 * Builds a storage reference to an object that is not yet stored in the blockchain,
	 * since it has been created during the current transaction.
	 *
	 * @param progressive the progressive number of the object among those that have been created
	 *                    during the same transaction
	 */
	public StorageReferenceInCurrentTransaction(BigInteger progressive) {
		super(progressive);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReferenceInCurrentTransaction && super.equals(other);
	}

	@Override
	public AbstractStorage deserialize(AbstractBlockchain blockchain) {
		// if the object is not yet in blockchain, it is not possible to deserialize it
		throw new DeserializationError("This reference identifies an object not yet in blockchain");
	}

	@Override
	public String toString() {
		return "THIS_TRANSACTION" + super.toString();
	}

	@Override
	public StorageReferenceAlreadyInBlockchain contextualizeAt(TransactionReference where) {
		// we assume the transaction is the given one
		return StorageReferenceAlreadyInBlockchain.mk(where, progressive);
	}

	@Override
	public String getClassName(AbstractBlockchain blockchain) {
		// if the object is not yet in blockchain, it is not possible to deserialize it and infer its class tag
		throw new DeserializationError("This reference identifies an object not yet in blockchain");
	}
}