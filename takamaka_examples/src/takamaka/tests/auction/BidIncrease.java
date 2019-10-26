package takamaka.tests.auction;

import java.math.BigInteger;

import io.takamaka.lang.Event;
import io.takamaka.lang.PayableContract;

public class BidIncrease extends Event {
	public final PayableContract caller;
	public final BigInteger amount;

	BidIncrease(PayableContract caller, BigInteger amount) {
		this.caller = caller;
		this.amount = amount;
	}
}