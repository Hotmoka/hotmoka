package takamaka.blockchain;

import java.math.BigInteger;

/**
 * A specification of the cost of gas for the transactions.
 */
public class GasCosts {

	/**
	 * The minimal number of units of gas charged for CPU usage for each transaction.
	 * Transactions with less gas will fail and will not be added to the blockchain.
	 */
	public final static BigInteger BASE_CPU_TRANSACTION_COST = BigInteger.valueOf(10L);

	/**
	 * The storage cost of a JVM slot for a variable inside a data structure.
	 */
	public static final BigInteger STORAGE_COST_PER_SLOT = BigInteger.valueOf(10L);

	/**
	 * The RAM cost of a single field of an object allocated in memory. This does not consider the
	 * objects reachable from the field, if any.
	 */
	public static final int RAM_COST_PER_FIELD = 4;

	/**
	 * The RAM cost of a single element of an array.
	 */
	public static final int RAM_COST_PER_ARRAY_SLOT = 4;

	/**
	 * The RAM cost of an array, without considering its elements.
	 */
	public static final int RAM_COST_PER_ARRAY = 8;

	/**
	 * The RAM cost of an activation record, without considering the variables therein.
	 */
	public static final long RAM_COST_PER_ACTIVATION_RECORD = 16;

	/**
	 * The RAM cost of a single variable inside an activation record.
	 */
	public static final long RAM_COST_PER_ACTIVATION_SLOT = 4;

	/**
	 * Provides the cost of a given amount of gas.
	 * 
	 * @param gas the amount of gas
	 * @return the cost
	 */
	public static BigInteger toCoin(BigInteger gas) {
		return gas.divide(BigInteger.valueOf(100));
	}

	/**
	 * Yields the storage gas cost for storing the given value.
	 * 
	 * @param value the value
	 * @return the cost
	 */
	public static BigInteger storageCostOf(BigInteger value) {
		return GasCosts.STORAGE_COST_PER_SLOT.add(BigInteger.valueOf(value.bitLength() / 32));
	}

	/**
	 * Yields the storage gas cost for storing the given value.
	 * 
	 * @param value the value
	 * @return the cost
	 */
	public static BigInteger storageCostOf(String value) {
		return GasCosts.STORAGE_COST_PER_SLOT.add(BigInteger.valueOf(value.length() / 4));
	}

	/**
	 * Yields the storage gas cost for installing in blockchain the given bytes.
	 * 
	 * @param jar the bytes
	 * @return the cost
	 */
	public static BigInteger storageCostForInstalling(byte[] jar) {
		return GasCosts.STORAGE_COST_PER_SLOT.add(BigInteger.valueOf(jar.length / 4));
	}

	/**
	 * Yields the CPU gas cost for installing in blockchain a jar consisting of the given bytes.
	 * 
	 * @param jar the bytes
	 * @return the cost
	 */
	public static BigInteger cpuCostForInstalling(byte[] jar) {
		return BigInteger.valueOf(jar.length / 400);
	}

	/**
	 * Yields the RAM gas cost for installing in blockchain a jar consisting of the given bytes.
	 * 
	 * @param jar the bytes
	 * @return the cost
	 */
	public static BigInteger ramCostForInstalling(byte[] jar) {
		return BigInteger.valueOf(jar.length / 40);
	}

	/**
	 * Yields the CPU gas cost for loading from blockchain a jar consisting of the given bytes.
	 * This happens during the construction of the class loader from blockchain.
	 * 
	 * @param jar the bytes
	 * @return the cost
	 */
	public static BigInteger cpuCostForLoading(byte[] jar) {
		return BigInteger.valueOf(jar.length / 1000);
	}

	/**
	 * Yields the RAM gas cost for loading from blockchain a jar consisting of the given bytes.
	 * This happens during the construction of the class loader from blockchain.
	 * 
	 * @param jar the bytes
	 * @return the cost
	 */
	public static BigInteger ramCostForLoading(byte[] jar) {
		return BigInteger.valueOf(jar.length / 200);
	}

	/**
	 * Yields the CPU cost for accessing from blockchain the request at the given transaction.
	 * 
	 * @param transaction the transaction
	 * @return the cost
	 */
	public static BigInteger cpuCostForGettingRequestAt(TransactionReference transaction) {
		return BigInteger.valueOf(10);
	}

	/**
	 * Yields the CPU cost for accessing from blockchain the response at the given transaction.
	 * 
	 * @param transaction the transaction
	 * @return the cost
	 */
	public static BigInteger cpuCostForGettingResponseAt(TransactionReference transaction) {
		return BigInteger.valueOf(10);
	}
}