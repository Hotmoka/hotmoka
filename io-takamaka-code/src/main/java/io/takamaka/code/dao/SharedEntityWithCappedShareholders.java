package io.takamaka.code.dao;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.takamaka.code.dao.SharedEntity.Offer;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;

/**
 * A shared entity with a capped number of shareholders.
 *
 * @param <S> the type of the shareholders
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class SharedEntityWithCappedShareholders<S extends PayableContract, O extends Offer<S>> extends SimpleSharedEntity<S, O> {

    /**
     * The maximal number of shareholders.
     */
    protected final int cap;

    /**
	 * Creates a shared entity with the given set of shareholders and respective shares.
	 *
	 * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
	 * @param shares the initial shares of each initial shareholder. This must be as many as {@code shareholders}
	 * @param cap the maximal number of shareholders
	 */
	public SharedEntityWithCappedShareholders(Stream<S> shareholders, Stream<BigInteger> shares, int cap) {
		super(shareholders, shares);

		require(getShares().size() <= cap, () -> "too many shareholders, the limit is " + cap);
		this.cap = cap;
	}

	/**
     * Creates a shared entity with the given set of shareholders and respective shares.
     *
     * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
     * @param shares       the initial shares of each initial shareholder. This must have the same length as {@code shareholders}
     * @param cap          the maximal number of shareholders
     */
    public SharedEntityWithCappedShareholders(S[] shareholders, BigInteger[] shares, int cap) {
    	this(Stream.of(shareholders), Stream.of(shares), cap);
    }

    /**
     * Creates a shared entity with one shareholder.
     *
     * @param shareholder the initial shareholder
     * @param share       the initial share of the initial shareholder
     * @param cap         the maximal number of shareholders
     */
    public SharedEntityWithCappedShareholders(S shareholder, BigInteger share, int cap) {
    	this(Stream.of(shareholder), Stream.of(share), cap);
    }

    /**
     * Creates a shared entity with two shareholders.
     *
     * @param shareholder1 the first initial shareholder
     * @param shareholder2 the second initial shareholder
     * @param share1       the initial share of the first shareholder
     * @param share2       the initial share of the second shareholder
     * @param cap          the maximal number of shareholders
     */
    public SharedEntityWithCappedShareholders(S shareholder1, S shareholder2, BigInteger share1, BigInteger share2, int cap) {
    	this(Stream.of(shareholder1, shareholder2), Stream.of(share1, share2), cap);
    }

    /**
     * Creates a shared entity with three shareholders.
     *
     * @param shareholder1 the first initial shareholder
     * @param shareholder2 the second initial shareholder
     * @param shareholder3 the third initial shareholder
     * @param share1       the initial share of the first shareholder
     * @param share2       the initial share of the second shareholder
     * @param share3       the initial share of the third shareholder
     * @param cap          the maximal number of shareholders
     */
    public SharedEntityWithCappedShareholders(S shareholder1, S shareholder2, S shareholder3, BigInteger share1, BigInteger share2, BigInteger share3, int cap) {
    	this(Stream.of(shareholder1, shareholder2, shareholder3), Stream.of(share1, share2, share3), cap);
    }

    /**
     * Creates a shared entity with four shareholders.
     *
     * @param shareholder1 the first initial shareholder
     * @param shareholder2 the second initial shareholder
     * @param shareholder3 the third initial shareholder
     * @param shareholder4 the fourth initial shareholder
     * @param share1       the initial share of the first shareholder
     * @param share2       the initial share of the second shareholder
     * @param share3       the initial share of the third shareholder
     * @param share4       the initial share of the fourth shareholder
     * @param cap          the maximal number of shareholders
     */
    public SharedEntityWithCappedShareholders(S shareholder1, S shareholder2, S shareholder3, S shareholder4, BigInteger share1, BigInteger share2, BigInteger share3, BigInteger share4, int cap) {
    	this(Stream.of(shareholder1, shareholder2, shareholder3, shareholder4), Stream.of(share1, share2, share3, share4), cap);
    }

    @Override
    public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, S buyer, O offer) {
        super.accept(amount, buyer, offer);
        require(getShares().size() <= cap, () -> "too many shareholders, the limit is " + cap);
    }
}