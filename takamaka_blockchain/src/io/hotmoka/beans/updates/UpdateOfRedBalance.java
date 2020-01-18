package io.hotmoka.beans.updates;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update that states the red balance of a red/green contract.
 */
@Immutable
public final class UpdateOfRedBalance extends UpdateOfField {

	private static final long serialVersionUID = -2091749852264921881L;

	/**
	 * The value set for the balance of the red/green contract.
	 */
	public final BigInteger balanceRed;

	/**
	 * Builds an update for the red balance of a red/green contract.
	 * 
	 * @param object the storage reference of the contract
	 * @param balanceRed the red balance set for the contract
	 */
	public UpdateOfRedBalance(StorageReference object, BigInteger balanceRed) {
		super(object);

		this.balanceRed = balanceRed;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfRedBalance && super.equals(other)
			&& ((UpdateOfRedBalance) other).balanceRed.equals(balanceRed);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ balanceRed.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return balanceRed.compareTo(((UpdateOfRedBalance) other).balanceRed);
	}

	@Override
	public StorageValue getValue() {
		return new BigIntegerValue(balanceRed);
	}

	@Override
	public FieldSignature getField() {
		return FieldSignature.RED_BALANCE_FIELD;
	}
}