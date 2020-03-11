package io.takamaka.tests.auction;

import java.math.BigInteger;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.PayableContract;

public class BidIncrease extends Event {
	public final PayableContract caller;
	public final BigInteger amount;

	BidIncrease(PayableContract caller, BigInteger amount) {
		this.caller = caller;
		this.amount = amount;
	}
}