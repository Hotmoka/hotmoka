package io.takamaka.code.blockchain.response;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * The response of a transaction.
 */
public interface TransactionResponse extends Serializable {

	/**
	 * The size of this response, in terms of storage gas units consumed if it is stored in blockchain.
	 * 
	 * @return the size
	 */
	BigInteger size();
}