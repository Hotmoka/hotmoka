package io.takamaka.code.dao;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;

/**
 * A shared entity where a shareholder must sell all its shares when it places an offer.
 *
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class SharedEntityWithIntegralShares<O extends SharedEntity.Offer> extends SimpleSharedEntity<O> {

    /**
     * Creates a shared entity with the given set of shareholders and respective shares.
     *
     * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
     * @param shares       the initial shares of each initial shareholder. This must have the same length as
     *                     {@code shareholders}
     */
    public SharedEntityWithIntegralShares(PayableContract[] shareholders, BigInteger[] shares) {
        super(shareholders, shares);
    }

    /**
     * Creates a shared entity with one shareholder.
     *
     * @param shareholder the initial shareholder
     * @param share       the initial share of the initial shareholder
     */
    public SharedEntityWithIntegralShares(PayableContract shareholder, BigInteger share) {
        this(new PayableContract[]{ shareholder }, new BigInteger[]{ share });
    }


    /**
     * Creates a shared entity with two shareholders.
     *
     * @param shareholder1 the first initial shareholder
     * @param shareholder2 the second initial shareholder
     * @param share1       the initial share of the first shareholder
     * @param share2       the initial share of the second shareholder
     */
    public SharedEntityWithIntegralShares(PayableContract shareholder1, PayableContract shareholder2, BigInteger share1, BigInteger share2) {
        this(new PayableContract[]{ shareholder1, shareholder2 }, new BigInteger[]{ share1, share2 });
    }

    @Override
    public @FromContract(PayableContract.class) @Payable void place(BigInteger amount, O offer) {
        require(sharesOf(offer.seller).equals(offer.sharesOnSale), "the seller must sell its shares integrally");
        super.place(amount, offer);
    }
}