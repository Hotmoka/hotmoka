package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.dao.SharedEntity;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.util.StorageMapView;

public class Validators extends SharedEntity<SharedEntity.Offer> {

	/**
	 * Creates a set of validators initialized with the given validators.
	 * 
	 * @param validators the initial validators in the set
	 */
	Validators(Validator[] validators, BigInteger[] powers) {
		super(validators, powers);
	}

	@Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, Offer offer) {
		// we ensure that the only shareholders are Validator's
		require(caller() instanceof Validator, () -> "only a " + Validator.class.getSimpleName() + " can accept an offer");
		super.accept(amount, offer);
	}

	/**
	 * Rewards validators that behaved correctly and punishes validators that
	 * misbehaved. Hotmoka nodes might call this method at regular
	 * intervals; for instance, after each committed block in a blockchain.
	 * Its goal is to reward the behaving validators and punish the
	 * misbehaving ones. Note that a validator might not be in
	 * {@code behaving} nor in {@code misbehaving} if, for instance, it
	 * failed to vote because it was down. The implementation of this
	 * method can decide what to do in that case.
	 * Normally, it is expected that the identifiers in {@code behaving}
	 * and {@code misbehaving} are those of validators in this validators set.
	 * 
	 * @param behaving space-separated identifiers of validators that behaved correctly
	 * @param misbehaving space-separated identifiers of validators that misbehaved
	 */
	public void reward(String behaving, String misbehaving) {
		// TODO: check that we are called as part of commit

		String[] behavingIDs = Manifest.splitAtSpaces(behaving);
		int howMany = behavingIDs.length;
		if (howMany > 0) {
			StorageMapView<PayableContract, BigInteger> shares = getShares();

			// compute the total power of the well behaving validators
			// that have been already revealed; this is always positive
			BigInteger totalPower = shares
				.keys()
				.map(shareholder -> (Validator) shareholder)
				.filter(shareholder -> contains(behavingIDs, shareholder))
				.map(shares::get)
				.reduce(ZERO, BigInteger::add);

			// distribute the balance of this contract to the well behaving validators
			// that have been already revealed, in proportion to their power
			BigInteger balance = balance();
			getShares()
				.keys()
				.map(shareholder -> (Validator) shareholder)
				.filter(shareholder -> contains(behavingIDs, shareholder))
				.forEachOrdered(shareholder -> shareholder.receive(balance.multiply(shares.get(shareholder)).divide(totalPower)));
		}
	}

	private static boolean contains(String[] IDs, Validator shareholder) {
		for (String id: IDs)
			if (shareholder.id.equals(id))
				return true;

		return false;
	}
}