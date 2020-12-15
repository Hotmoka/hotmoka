package io.takamaka.code.dao;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;

import java.math.BigInteger;

import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

/**
 * A shared entity where there is a max shareholder limit for an entity
 *
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class SharedEntityMaxShareholders<O extends SharedEntity.Offer> extends SharedEntity<O> {

    /**
     * The maximum number of share for the shared entity
     */
    protected final int ShareholderLimit;

    /**
     * Creates a shared entity with the given set of shareholders and respective shares.
     *
     * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
     * @param shares       the initial shares of each initial shareholder. This must have the same length as
     *                     {@code shareholders}
     * @param limit        the maximum shareholder limit for this entity
     */
    public SharedEntityMaxShareholders(PayableContract[] shareholders, BigInteger[] shares, int limit) {
        super(shareholders, shares);
        require(getShares().size() <= limit, "shareholder limit exceeded");
        ShareholderLimit = limit;
    }

    /**
     * Called whenever an offer is being accepted. By default, this method
     * deletes the offer and transfers the shares. If there are too many shareholders
     * then the offer is not accepted.
     *
     * @param amount the ticket payed for accepting the offer; this must at least
     *               pay for the cost of {@code offer}, but subclasses may require
     *               to pay an extra ticket
     * @param offer  the accepted offer
     */
    @Override
    public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, O offer) {
        require(getShares().get(offer.seller).subtract(offer.sharesOnSale).equals(ZERO) ||
                !getShares().getOrDefault(caller(), ZERO).equals(ZERO) ||
                getShares().size() + 1 <= ShareholderLimit, "shareholder limit reached");
        super.accept(amount, offer);
    }
}
