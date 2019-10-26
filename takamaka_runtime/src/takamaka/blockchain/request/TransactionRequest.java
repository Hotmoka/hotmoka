package takamaka.blockchain.request;

import java.io.Serializable;
import java.math.BigInteger;

import io.takamaka.code.annotations.Immutable;
import takamaka.blockchain.UpdateOfBalance;

@Immutable
public interface TransactionRequest extends Serializable {

	/**
	 * The size of this request, in terms of storage gas units consumed if it is stored in blockchain.
	 * 
	 * @return the size
	 */
	BigInteger size();

	/*
	 * Checks that this request has promised the minimal amount of gas required to run it. If less gas is provided,
	 * the transaction will not even start, will generate a {@link takamaka.blockchain.TransactionException}
	 * and will not be added to blockchain. The minimal gas accounts for the expansion of the blockchain
	 * with this request and the corresponding failed response, at least.
	 * 
	 * @param balanceUpdateInCaseOfFailure the update of the balance field of the caller, if the transaction fails
	 * @return true if and only if the gas is enough at least to start the transaction
	 */
	boolean hasMinimalGas(UpdateOfBalance balanceUpdateInCaseOfFailure);
}