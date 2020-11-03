package io.takamaka.tests.auction;

import java.math.BigInteger;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;

public class BidIncrease extends Event {
	public final PayableContract bidder;
	public final BigInteger amount;

	@Entry BidIncrease(PayableContract bidder, BigInteger amount) {
		this.bidder = bidder;
		this.amount = amount;
	}

	public @View PayableContract getBidder() {
		return bidder;
	}

	public @View BigInteger getAmount() {
		return amount;
	}
}