package io.hotmoka.examples.sharedentities;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageSet;
import io.takamaka.code.util.StorageSetView;
import io.takamaka.code.util.StorageTreeMap;
import io.takamaka.code.util.StorageTreeSet;

/**
 * A simple implementation of a shared entity. Shareholders hold, sell and buy shares of a shared entity.
 * Selling and buying shares do not require to pay a ticket. It uses a non-generic type for the shareholders.
 * 
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class SimpleSharedEntity1<O extends SharedEntity1.Offer> extends PayableContract implements SharedEntity1<O> {

	/**
	 * The shares of each shareholder. These are always positive.
	 */
	private final StorageTreeMap<PayableContract, BigInteger> shares = new StorageTreeMap<>();

	/**
	 * The set of offers of sale of shares.
	 */
	private final StorageSet<O> offers = new StorageTreeSet<>();

	/**
	 * A snapshot of the current shares.
	 */
	private StorageMapView<PayableContract, BigInteger> snapshotOfShares;

	/**
	 * A snapshot of the current offers.
	 */
	private StorageSetView<O> snapshotOfOffers;

	/**
	 * Creates a shared entity with the given set of shareholders and respective shares.
	 * 
	 * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
	 * @param shares the initial shares of each initial shareholder. This must have the same length as {@code shareholders}
	 */
	public SimpleSharedEntity1(PayableContract[] shareholders, BigInteger[] shares) {
		require(shareholders != null, "shareholders cannot be null");
		require(shares != null, "shares cannot be null");
		require(shareholders.length == shares.length, "shareholders and shares must have the same length");

		int pos = 0;
		for (PayableContract shareholder: shareholders) {
			require(shareholder != null, "shareholders cannot be null");
			BigInteger added = shares[pos++];
			require(added != null && added.signum() > 0, "shares must be positive big integers");
			addShares(shareholder, added);
		}

		this.snapshotOfShares = this.shares.snapshot();
		this.snapshotOfOffers = offers.snapshot();
	}

	/**
	 * Creates a shared entity with one shareholder.
	 *
	 * @param shareholder the initial shareholder
	 * @param share the initial share of the initial shareholder
	 */
	public SimpleSharedEntity1(PayableContract shareholder, BigInteger share) {
		this(new PayableContract[]{shareholder}, new BigInteger[]{share});
	}

	/**
     * Creates a shared entity with two shareholders.
     *
     * @param shareholder1 the first initial shareholder
     * @param shareholder2 the second initial shareholder
     * @param share1       the initial share of the first shareholder
     * @param share2       the initial share of the second shareholder
     */
    public SimpleSharedEntity1(PayableContract shareholder1, PayableContract shareholder2, BigInteger share1, BigInteger share2) {
        this(new PayableContract[]{ shareholder1, shareholder2 }, new BigInteger[]{ share1, share2 });
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
     */
    public SimpleSharedEntity1(PayableContract shareholder1, PayableContract shareholder2, PayableContract shareholder3, BigInteger share1, BigInteger share2, BigInteger share3) {
        this(new PayableContract[]{ shareholder1, shareholder2, shareholder3 }, new BigInteger[]{ share1, share2, share3 });
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
     */
    public SimpleSharedEntity1(PayableContract shareholder1, PayableContract shareholder2, PayableContract shareholder3, PayableContract shareholder4, BigInteger share1, BigInteger share2, BigInteger share3, BigInteger share4) {
        this(new PayableContract[]{ shareholder1, shareholder2, shareholder3, shareholder4 }, new BigInteger[]{ share1, share2, share3, share4 });
    }

    @Override
	public @View final StorageSetView<O> getOffers() {
		return snapshotOfOffers;
	}

    @Override
	public @View final StorageMapView<PayableContract, BigInteger> getShares() {
		return snapshotOfShares;
	}

    @Override
    public @View final boolean isShareholder(Object who) {
    	return snapshotOfShares.containsKey(who);
    }

    @Override
	public final Stream<PayableContract> getShareholders() {
		return snapshotOfShares.keys();
	}

    @Override
	public final @View BigInteger sharesOf(PayableContract shareholder) {
		return shares.getOrDefault(shareholder, ZERO);
	}

    @Override
	public final @View BigInteger sharesOnSaleOf(PayableContract shareholder) {
		return offers.stream()
			.filter(offer -> offer.seller == shareholder && offer.isOngoing())
			.map(offer -> offer.sharesOnSale)
			.reduce(ZERO, BigInteger::add);
	}

    @Override
	public @FromContract(PayableContract.class) @Payable void place(BigInteger amount, O offer) {
		PayableContract seller = (PayableContract) caller();
		require(offer.seller == seller, "only the seller can place its own offer");
		require(shares.containsKey(seller), "the seller is not a shareholder");
		require(sharesOf(seller).subtract(sharesOnSaleOf(seller)).compareTo(offer.sharesOnSale) >= 0, "the seller has not enough shares to sell");
		cleanUpOffers(null);
		offers.add(offer);
		snapshotOfOffers = offers.snapshot();
		event(new OfferPlaced(offer));
	}

    @Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, O offer) {
		require(offers.contains(offer), "unknown offer");
		require(offer.isOngoing(), "the sale offer is not ongoing anymore");
		require(offer.cost.compareTo(amount) <= 0, "not enough money to accept the offer");
		PayableContract buyer = (PayableContract) caller();
		cleanUpOffers(offer);
		removeShares(offer.seller, offer.sharesOnSale);
		addShares(buyer, offer.sharesOnSale);
		offer.seller.receive(offer.cost);
		snapshotOfShares = shares.snapshot();
		event(new OfferAccepted(buyer, offer));
	}

	/**
	 * Deletes offers that have expired.
	 * 
	 * @param offerToRemove an offer whose first occurrence must be removed
	 */
	private void cleanUpOffers(O offerToRemove) {
		offers.stream()
			.filter(offer -> offer == offerToRemove || !offer.isOngoing())
			.forEachOrdered(offers::remove);
	}

	private void addShares(PayableContract shareholder, BigInteger added) {
		shares.update(shareholder,
			() -> {
				event(new ShareholderAdded(shareholder));
				return ZERO;
			},
			added::add);
	}

	private void removeShares(PayableContract shareholder, BigInteger removed) {
		shares.update(shareholder, shares -> shares.subtract(removed));
		if (shares.get(shareholder).signum() == 0) {
			shares.remove(shareholder);
			event(new ShareholderRemoved(shareholder));
		}
	}
}