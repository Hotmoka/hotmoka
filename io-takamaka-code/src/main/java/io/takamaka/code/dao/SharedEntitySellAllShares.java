package io.takamaka.code.dao;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;

import java.math.BigInteger;

import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

/**
 * A shared entity where a shareholder must sell all his shares when he places an offer
 *
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class SharedEntitySellAllShares<O extends SharedEntity.Offer> extends SharedEntity<O> {

    /**
     * Creates a shared entity with the given set of shareholders and respective shares.
     *
     * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
     * @param shares       the initial shares of each initial shareholder. This must have the same length as
     *                     {@code shareholders}
     */
    public SharedEntitySellAllShares(PayableContract[] shareholders, BigInteger[] shares) {
        super(shareholders, shares);
    }

    /**
     * Creates a shared entity with one shareholder and the respective share.
     *
     * @param shareholder the initial shareholder
     * @param share       the initial share of the initial shareholder
     */
    public SharedEntitySellAllShares(PayableContract shareholder, BigInteger share) {
        this(new PayableContract[]{shareholder}, new BigInteger[]{share});
    }

    /**
     * Place an offer of sale of shares for this entity. By default, this method checks
     * the offer, adds it to the current offers and issues an event. If the seller does not
     * sell all of his shares, the offer is not added.
     *
     * @param amount the ticket payed to place the offer; this can be zero, but subclasses
     *               may require differently
     * @param offer  the offer that is going to be placed
     */
    @Override
    public @FromContract(PayableContract.class) @Payable void place(BigInteger amount, O offer) {
        require(getShares().get(offer.seller).subtract(offer.sharesOnSale).equals(ZERO), "the seller must sell all shares");

        super.place(amount, offer);
    }
}
