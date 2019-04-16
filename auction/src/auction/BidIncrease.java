package auction;

import takamaka.lang.Contract;
import takamaka.lang.Event;

public class BidIncrease extends Event {
	public final Contract caller;
	public final int amount;

	BidIncrease(Contract caller, int amount) {
		this.caller = caller;
		this.amount = amount;
	}
}