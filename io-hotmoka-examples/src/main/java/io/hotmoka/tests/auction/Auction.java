package io.hotmoka.tests.auction;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.PayableContract;

public abstract class Auction extends Contract {

	/**
	 * End the auction and send the highest bid to the beneficiary.
	 * 
	 * @return the winner of the action
	 */
	public abstract PayableContract auctionEnd();
}