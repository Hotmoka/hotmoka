import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.lang.Storage;
import takamaka.util.StorageList;

public class CrowdFunding extends Contract {
	private final StorageList<Campaign> campaigns = new StorageList<>();

	public int newCampaign(PayableContract beneficiary, BigInteger goal) {
		campaigns.add(new Campaign(beneficiary, goal));
		return campaigns.size() - 1;
	}

	public @Payable @Entry void contribute(BigInteger amount, int campaignID) {
		Campaign c = campaigns.get(campaignID);
		c.funders.add(new Funder(caller(), amount));
		c.amount = c.amount.add(amount);
	}

	public boolean checkGoalReached(int campaignID) {
		Campaign c = campaigns.get(campaignID);
		if (c.amount.compareTo(c.fundingGoal) < 0)
			return false;
		else {
			BigInteger amount = c.amount;
			c.amount = BigInteger.ZERO;
			c.beneficiary.receive(amount);
			return true;
		}
	}

	private static class Campaign extends Storage {
		private final PayableContract beneficiary;
		private final BigInteger fundingGoal;
		private final StorageList<Funder> funders = new StorageList<>();
		private BigInteger amount;

		private Campaign(PayableContract beneficiary, BigInteger fundingGoal) {
			this.beneficiary = beneficiary;
			this.fundingGoal = fundingGoal;
		}
	}
}