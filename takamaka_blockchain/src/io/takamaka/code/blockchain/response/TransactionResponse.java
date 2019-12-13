package io.takamaka.code.blockchain.response;

import java.io.Serializable;
import java.math.BigInteger;

import io.takamaka.code.blockchain.GasCostModel;

/**
 * The response of a transaction.
 */
public interface TransactionResponse extends Serializable {

	/**
	 * The size of this response, in terms of storage gas units consumed if it is stored in blockchain.
	 * 
	 * @param gasCostModel the gas cost model of the blockchain
	 * @return the size
	 */
	BigInteger size(GasCostModel gasCostModel);
}