package io.hotmoka.beans.responses;

import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * A response for a non-initial transaction.
 */
@Immutable
public abstract class NonInitialTransactionResponse extends TransactionResponse {

	/**
	 * Yields the size of this response, in terms of gas units consumed in store.
	 * 
	 * @param gasCostModel the model of the costs
	 * @return the size
	 */
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot());
	}
}