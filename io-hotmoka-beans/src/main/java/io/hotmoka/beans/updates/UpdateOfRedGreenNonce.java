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
 * An update that states the nonce of a red/green externally-owned account.
 */
@Immutable
public final class UpdateOfRedGreenNonce extends UpdateOfField {
	final static byte SELECTOR = 14;

	/**
	 * The value set for the nonce of the account.
	 */
	public final BigInteger nonce;

	/**
	 * Builds an update for the nonce of a red/green externally-owned account.
	 * 
	 * @param object the storage reference of the contract
	 * @param nonce the nonce set for the account
	 */
	public UpdateOfRedGreenNonce(StorageReference object, BigInteger nonce) {
		super(object);

		this.nonce = nonce;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfRedGreenNonce && super.equals(other)
			&& ((UpdateOfRedGreenNonce) other).nonce.equals(nonce);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ nonce.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return nonce.compareTo(((UpdateOfRedGreenNonce) other).nonce);
	}

	@Override
	public StorageValue getValue() {
		return new BigIntegerValue(nonce);
	}

	@Override
	public FieldSignature getField() {
		return FieldSignature.RGEOA_NONCE_FIELD;
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(nonce));
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		marshal(nonce, oos);
	}
}