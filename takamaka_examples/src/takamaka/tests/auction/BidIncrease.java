package takamaka.tests.auction;

import java.math.BigInteger;

import takamaka.lang.Event;
import takamaka.lang.PayableContract;

public class BidIncrease extends Event {
	public final PayableContract caller;
	public final BigInteger amount;

	BidIncrease(PayableContract caller, BigInteger amount) {
		this.caller = caller;
		this.amount = amount;
	}
}