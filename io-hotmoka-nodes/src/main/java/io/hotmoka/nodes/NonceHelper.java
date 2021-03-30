package io.hotmoka.nodes;

import java.math.BigInteger;
import java.util.NoSuchElementException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * An object that helps with nonce operations.
 */
public class NonceHelper {
	private final Node node;
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * Creates an object that helps with nonce operations.
	 * 
	 * @param node the node whose accounts are considered
	 */
	public NonceHelper(Node node) {
		this.node = node;
	}

	/**
	 * Yields the nonce of an account.
	 * 
	 * @param account the account
	 * @return the nonce of {@code account}
	 */
	public BigInteger getNonceOf(StorageReference account) throws TransactionRejectedException, NoSuchElementException, TransactionException, CodeExecutionException {
		// we ask the account: 10,000 units of gas should be enough to run the method
		return ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(account, _100_000, node.getClassTag(account).jar, CodeSignature.NONCE, account))).value;
	}
}