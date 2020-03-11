package io.takamaka.tests.ponzi;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;

/**
 * A contract for a Ponzi investment scheme:
 * It involves taking the money sent by the current investor
 * and transferring it to the previous investor. As long as
 * the investment is larger than the previous one, every investor
 * except the last will get a return on their investment.
 * 
 * This example is translated from a Solidity contract shown
 * in "Building Games with Ethereum Smart Contracts", by Iyer and Dannen,
 * page 145, Apress 2018.
 */
public class SimplePonzi extends Contract {
	private final BigInteger _10 = BigInteger.valueOf(10L);
	private final BigInteger _11 = BigInteger.valueOf(11L);
	private PayableContract currentInvestor;
	private BigInteger currentInvestment = BigInteger.ZERO;

	public @Payable @Entry(PayableContract.class) void invest(BigInteger amount) {
		// new investments must be 10% greater than current
		BigInteger minimumInvestment = currentInvestment.multiply(_11).divide(_10);
		require(amount.compareTo(minimumInvestment) > 0, () -> "you must invest more than " + minimumInvestment);

		// document new investor
		currentInvestor.receive(amount);
		currentInvestor = (PayableContract) caller();
		currentInvestment = amount;
	}
}