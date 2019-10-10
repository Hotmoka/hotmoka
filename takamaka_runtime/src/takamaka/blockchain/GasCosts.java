package takamaka.blockchain;

import java.math.BigInteger;

import org.apache.bcel.generic.Instruction;


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
	 * The minimal number of units of gas charged for storage consumption for each transaction.
	 * Transactions with less gas will fail and will not be added to the blockchain.
	 */
	public final static BigInteger BASE_STORAGE_TRANSACTION_COST = BigInteger.valueOf(90L);

	/**
	 * The units of gas charged for storage consumption for each byte of a jar installed during a
	 * jar installation transaction. This adds to the base transaction cost.
	 * It refers to the number of bytes of the jar sent along the transaction,
	 * not to the instrumented jar.
	 */
	public final static double STORAGE_COST_PER_BYTE_IN_JAR = 0.1f;

	/**
	 * The units of gas charged for storage consumption for each dependency of a jar installed during
	 * jar installation transaction.
	 */
	public final static BigInteger STORAGE_COST_PER_DEPENDENCY_OF_JAR = BigInteger.valueOf(1L);

	/**
	 * The units of gas charged for storage consumption for each unit of updates generated
	 * at the end of a transaction.
	 */
	public static final BigInteger STORAGE_COST_PER_UNIT_OF_UPDATE = BigInteger.valueOf(5L);

	/**
	 * The units of gas charged for storage consumption for each event generated
	 * during a transaction. This is a constant since it only accounts for a reference
	 * to the update, which is otherwise account for in the updates of the transaction.
	 */
	public static final BigInteger STORAGE_COST_PER_EVENT = BigInteger.valueOf(2L);

	/**
	 * Provides the cost of a given amount of gas.
	 * 
	 * @param gas the amount of gas
	 * @return the cost
	 */
	public static BigInteger toCoin(BigInteger gas) {
		return gas.divide(BigInteger.TEN);
	}

	/**
	 * Yields the gas cost of the execution of the given bytecode instruction.
	 * 
	 * @param bytecode the instruction
	 * @return the cost. This should be below 2 to the power of 47
	 */
	public static long costOf(Instruction bytecode) {
		return 1L; //TODO
	}
}