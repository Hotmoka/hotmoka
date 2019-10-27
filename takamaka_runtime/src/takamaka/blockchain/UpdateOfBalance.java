package takamaka.blockchain;

import java.math.BigInteger;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;

/**
 * An update that states the balance of a contract.
 */
@Immutable
public final class UpdateOfBalance extends UpdateOfField {

	private static final long serialVersionUID = -1880321211899442705L;

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
	public UpdateOfBalance contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfBalance(objectContextualized, balance);
		else
			return this;
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
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(GasCosts.storageCostOf(balance));
	}
}