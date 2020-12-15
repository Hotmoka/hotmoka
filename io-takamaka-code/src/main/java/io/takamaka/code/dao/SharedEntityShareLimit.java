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
     * Creates a shared entity with the given set of shareholders and respective shares.
     *
     * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
     * @param shares       the initial shares of each initial shareholder. This must have the same length as
     *                     {@code shareholders}
     * @param shareLimit   the share limit for a single shareholder in percentage
     */
    public SharedEntityShareLimit(PayableContract[] shareholders, BigInteger[] shares, int shareLimit) {
        super(shareholders, shares);
        require(shareLimit < 100 && shareLimit > 0, "invalid share limit");

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
