package io.takamaka.tests.auction;

import java.math.BigInteger;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.PayableContract;

public class AuctionEnd extends Event {
	public final PayableContract highestBidder;
	public final BigInteger highestBid;

	AuctionEnd(PayableContract highestBidder, BigInteger highestBid) {
		this.highestBidder = highestBidder;
		this.highestBid = highestBid;
	}
}