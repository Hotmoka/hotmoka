package takamaka.blockchain;

import java.math.BigInteger;

public class GasCosts {
	/**
	 * The minimal number of units of gas charged for a transaction. If the
	 * transaction succeeds, extra charge will be added to this constant.
	 * If the transaction fails, this basic cost is charged.
	 */
	public final static int BASE_TRANSACTION_COST = 100;

	/**
	 * The units of gas charged for each byte of a jar installed during a
	 * jar installation transaction. This adds to the base transaction cost.
	 * It refers to the number of bytes of the jar sent along the transaction,
	 * not to the instrumented jar.
	 */
	public final static double GAS_PER_BYTE_IN_JAR = 0.1f;

	/**
	 * The units of gas charged for each dependency of a jar installed during
	 *  jar installation transaction.
	 */
	public final static double GAS_PER_DEPENDENCY_OF_JAR = 1000f;

	public static BigInteger toCoin(long gas) {
		return BigInteger.valueOf((long)(gas * 0.1));
	}
}