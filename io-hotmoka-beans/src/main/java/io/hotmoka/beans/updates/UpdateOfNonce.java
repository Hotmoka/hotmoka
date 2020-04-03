package io.hotmoka.beans.updates;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update that states the nonce of an externally-owned account.
 */
@Immutable
public final class UpdateOfNonce extends UpdateOfField {

	private static final long serialVersionUID = 6043020907916137622L;

	/**
	 * The value set for the nonce of the account.
	 */
	public final BigInteger nonce;

	/**
	 * Builds an update for the nonce of an externally-owned account.
	 * 
	 * @param object the storage reference of the contract
	 * @param nonce the nonce set for the account
	 */
	public UpdateOfNonce(StorageReference object, BigInteger nonce) {
		super(object);

		this.nonce = nonce;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfNonce && super.equals(other)
			&& ((UpdateOfNonce) other).nonce.equals(nonce);
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
			return nonce.compareTo(((UpdateOfNonce) other).nonce);
	}

	@Override
	public StorageValue getValue() {
		return new BigIntegerValue(nonce);
	}

	@Override
	public FieldSignature getField() {
		return FieldSignature.EOA_NONCE_FIELD;
	}
}