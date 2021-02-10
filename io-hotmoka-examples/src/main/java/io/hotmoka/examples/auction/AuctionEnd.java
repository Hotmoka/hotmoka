package io.hotmoka.examples.auction;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;

public class AuctionEnd extends Event {
	public final PayableContract highestBidder;
	public final BigInteger highestBid;

	@FromContract AuctionEnd(PayableContract highestBidder, BigInteger highestBid) {
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