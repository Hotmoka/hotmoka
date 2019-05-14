package auction;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Event;

public class BidIncrease extends Event {
	public final Contract caller;
	public final BigInteger amount;

	BidIncrease(Contract caller, BigInteger amount) {
		this.caller = caller;
		this.amount = amount;
	}
}