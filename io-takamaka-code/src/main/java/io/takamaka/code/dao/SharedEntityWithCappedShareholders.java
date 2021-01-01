package io.takamaka.code.dao;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;

/**
 * A shared entity with a capped number of shareholders.
 *
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class SharedEntityWithCappedShareholders<O extends SharedEntity.Offer> extends SharedEntity<O> {

    /**
     * The maximal number of shareholders.
     */
    protected final int cap;

    /**
     * Creates a shared entity with the given set of shareholders and respective shares.
     *
     * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
     * @param shares       the initial shares of each initial shareholder. This must have the same length as
     *                     {@code shareholders}
     * @param cap          the maximal number of shareholders
     */
    public SharedEntityWithCappedShareholders(PayableContract[] shareholders, BigInteger[] shares, int cap) {
        super(shareholders, shares);

        require(getShares().size() <= cap, () -> "too many shareholders, the limit is " + cap);
        this.cap = cap;
    }

    /**
     * Creates a shared entity with one shareholder.
     *
     * @param shareholder the initial shareholder
     * @param share       the initial share of the initial shareholder
     * @param cap         the maximal number of shareholders
     */
    public SharedEntityWithCappedShareholders(PayableContract shareholder, BigInteger share, int cap) {
        this(new PayableContract[]{ shareholder }, new BigInteger[]{ share }, cap);
    }

    @Override
    public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, O offer) {
        super.accept(amount, offer);
        require(getShares().size() <= cap, () -> "too many shareholders, the limit is " + cap);
    }
}