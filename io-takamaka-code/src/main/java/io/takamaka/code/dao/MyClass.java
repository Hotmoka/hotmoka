package io.takamaka.code.dao;

import io.takamaka.code.lang.*;

import java.math.BigInteger;

/**
 * A test contract that represents a shareholder
 */
public class MyClass extends PayableContract {

    /**
     * Simple and basic constructor that refers to the Contract one
     */
    public MyClass() {
        super();
    }

    /**
     * Create an offer using this contract
     *
     * @param sharesOnSale the shares on sale, positive
     * @param cost the cost, non-negative
     * @param duration the duration of validity of the offer, in milliseconds from now, always non-negative
     * @return the created offer
     */
    public @FromContract(PayableContract.class) SharedEntity2.Offer createOffer(BigInteger sharesOnSale, BigInteger cost, long duration) {
        return new SharedEntity2.Offer(sharesOnSale, cost, duration);
    }

    /**
     * Place an offer of sale of shares for a shared entity
     *
     * @param sh the shared entity where the offer will be placed
     * @param amount the ticket payed to place the offer; implementations may allow zero for this
     * @param offer the offer that is going to be placed
     */
    public @FromContract(PayableContract.class) void placeOffer(SharedEntity2 sh, BigInteger amount, SharedEntity2.Offer offer) {
        sh.place(amount, offer);
    }

}
