package io.hotmoka.beans.updates;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update that states the balance of a contract.
 */
@Immutable
public final class UpdateOfBalance extends UpdateOfField {
	final static byte SELECTOR = 1;

	/**
	 * The value set for the balance of the contract.
	 */
	public final BigInteger balance;

	/**
	 * Builds an update for the balance of a contract.
	 * 
	 * @param object the storage reference of the contract
	 * @param balance the balance set for the contract
	 */
	public UpdateOfBalance(StorageReference object, BigInteger balance) {
		super(object);

		this.balance = balance;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfBalance && super.equals(other)
			&& ((UpdateOfBalance) other).balance.equals(balance);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ balance.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return balance.compareTo(((UpdateOfBalance) other).balance);
	}

	@Override
	public StorageValue getValue() {
		return new BigIntegerValue(balance);
	}

	@Override
	public FieldSignature getField() {
		return FieldSignature.BALANCE_FIELD;
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(balance));
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		marshal(balance, oos);
	}
}