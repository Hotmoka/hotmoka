package io.hotmoka.examples.sharedentities;

import java.math.BigInteger;

import io.takamaka.code.dao.SharedEntity;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.PayableContract;

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
     * Create an offer using this contract (SharedEntity2.Offer or SharedEntity3.Offer)
     *
     * @param sharesOnSale the shares on sale, positive
     * @param cost the cost, non-negative
     * @param duration the duration of validity of the offer, in milliseconds from now, always non-negative
     * @return the created offer
     */
    public @FromContract(PayableContract.class) SharedEntity2.Offer<MyClass> createOffer2(BigInteger sharesOnSale, BigInteger cost, long duration) {
        return new SharedEntity2.Offer<>(sharesOnSale, cost, duration);
    }

    public @FromContract(PayableContract.class) SharedEntity1.Offer createOffer1(BigInteger sharesOnSale, BigInteger cost, long duration) {
        return new SharedEntity1.Offer(sharesOnSale, cost, duration);
    }

    public @FromContract(PayableContract.class) SharedEntity.Offer<MyClass> createOffer(BigInteger sharesOnSale, BigInteger cost, long duration) {
        return new SharedEntity.Offer<>(this, sharesOnSale, cost, duration);
    }

    /**
     * Place an offer of sale of shares for a shared entity.
     *
     * @param sh the shared entity where the offer will be placed
     * @param amount the ticket payed to place the offer; implementations may allow zero for this
     * @param offer the offer that is going to be placed
     */
    public @FromContract(PayableContract.class) void placeOffer(SharedEntity2<MyClass, SharedEntity2.Offer<MyClass>> sh, BigInteger amount, SharedEntity2.Offer<MyClass> offer) {
        sh.place(amount, offer);
    }

    public @FromContract(PayableContract.class) void placeOffer(SharedEntity1<SharedEntity1.Offer> sh, BigInteger amount, SharedEntity1.Offer offer) {
        sh.place(amount, offer);
    }

    public @FromContract(PayableContract.class) void placeOffer(SharedEntity<MyClass, SharedEntity.Offer<MyClass>> sh, BigInteger amount, SharedEntity.Offer<MyClass> offer) {
        sh.place(amount, offer);
    }

//    public @FromContract(PayableContract.class) void placeOffer(MyClassSharedEntity<SharedEntity3.Offer<MyClass>> sh, BigInteger amount, SharedEntity3.Offer<MyClass> offer) {
//        sh.place(amount, offer);
//    }

}
