package io.takamaka.tests.auction;

import java.math.BigInteger;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.PayableContract;

public class AuctionEnd extends Event {
	public final PayableContract highestBidder;
	public final BigInteger highestBid;

	AuctionEnd(Auction auction, PayableContract highestBidder, BigInteger highestBid) {
		super(auction);

		this.highestBidder = highestBidder;
		this.highestBid = highestBid;
	}
}