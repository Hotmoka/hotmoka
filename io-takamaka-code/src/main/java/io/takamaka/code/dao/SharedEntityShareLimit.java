package io.takamaka.code.dao;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;

import java.math.BigInteger;

import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

/**
 * A shared entity where each shareholder can't own more than a percentage of shares
 *
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class SharedEntityShareLimit<O extends SharedEntity.Offer> extends SharedEntity<O> {

    /**
     * The share limit that a single shareholder can own (in number of shares)
     */
    private final BigInteger ShareLimit;

    /**
     * Creates a shared entity with the given set of shareholders, respective shares and a share limit
     * for a single shareholder.
     *
     * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
     * @param shares       the initial shares of each initial shareholder. This must have the same length as
     *                     {@code shareholders}
     * @param shareLimit   the share limit for a single shareholder in percentage
     */
    public SharedEntityShareLimit(PayableContract[] shareholders, BigInteger[] shares, int shareLimit) {
        super(shareholders, shares);
        require(shareLimit <= 100 && shareLimit > 0, "invalid share limit");

        BigInteger TotalShares = ZERO;

        for (BigInteger s : shares) {
            TotalShares = TotalShares.add(s);
        }

        ShareLimit = TotalShares.multiply(BigInteger.valueOf(shareLimit)).divide(BigInteger.valueOf(100));

        for (PayableContract sh : shareholders) {
            require(getShares().getOrDefault(sh, ZERO).compareTo(ShareLimit) <= 0, "shareholder exceeded share limit");
        }
    }

    /**
     * Creates a shared entity with one shareholder, the respective share and a share limit for a single shareholder.
     *
     * @param shareholder the initial shareholder
     * @param share       the initial share of the initial shareholder
     * @param shareLimit  the share limit for a single shareholder in percentage
     */
    public SharedEntityShareLimit(PayableContract shareholder, BigInteger share, int shareLimit) {
        this(new PayableContract[]{shareholder}, new BigInteger[]{share}, shareLimit);
    }

    /**
     * Creates a shared entity with two shareholders, the respective shares and a share limit for a single shareholder.
     *
     * @param shareholder1 the first initial shareholder
     * @param shareholder2 the second initial shareholder
     * @param share1       the initial share of the first shareholder
     * @param share2       the initial share of the second shareholder
     * @param shareLimit   the share limit for a single shareholder in percentage
     */
    public SharedEntityShareLimit(PayableContract shareholder1, PayableContract shareholder2, BigInteger share1, BigInteger share2, int shareLimit) {
        this(new PayableContract[]{shareholder1, shareholder2}, new BigInteger[]{share1, share2}, shareLimit);
    }

    /**
     * Called whenever an offer is being accepted. By default, this method
     * deletes the offer and transfers the shares. If the buyer exceeds the
     * share limit then the offer is not accepted.
     *
     * @param amount the ticket payed for accepting the offer; this must at least
     *               pay for the cost of {@code offer}, but subclasses may require
     *               to pay an extra ticket
     * @param offer  the accepted offer
     */
    @Override
    public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, O offer) {
        require(getShares().getOrDefault(caller(), ZERO).add(offer.sharesOnSale).compareTo(ShareLimit) <= 0, "shareholder cannot exceed share limit");
        super.accept(amount, offer);
    }
}
