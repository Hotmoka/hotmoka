package io.takamaka.tests.auction;

import java.math.BigInteger;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;

public class AuctionEnd extends Event {
	public final PayableContract highestBidder;
	public final BigInteger highestBid;

	@Entry AuctionEnd(PayableContract highestBidder, BigInteger highestBid) {
		this.highestBidder = highestBidder;
		this.highestBid = highestBid;
	}

	public @View PayableContract getHighestBidder() {
		return highestBidder;
	}

	public @View BigInteger getHighestBid() {
		return highestBid;
	}
}