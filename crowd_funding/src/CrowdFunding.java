import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.lang.Storage;
import takamaka.util.StorageList;

public class CrowdFunding extends Contract {
	private final StorageList<Campaign> campaigns = new StorageList<>();

	public int newCampaign(PayableContract beneficiary, int goal) {
		int campaignId = campaigns.size();
		campaigns.add(new Campaign(beneficiary, goal));
		return campaignId;
	}

	public @Payable @Entry void contribute(int amount, int campaignID) {
		campaigns.get(campaignID).addFunder(caller(), amount);
	}

	public boolean checkGoalReached(int campaignID) {
		return campaigns.get(campaignID).payIfGoalReached();
	}

	private void pay(PayableContract whom, int amount) {
		whom.receive(amount);
	}

	private class Campaign extends Storage {
		private final PayableContract beneficiary;
		private final int fundingGoal;
		private final StorageList<Funder> funders = new StorageList<>();
		private int amount;

		private Campaign(PayableContract beneficiary, int fundingGoal) {
			this.beneficiary = beneficiary;
			this.fundingGoal = fundingGoal;
		}

		private void addFunder(Contract who, int amount) {
			this.funders.add(new Funder(who, amount));
			this.amount += amount;
		}

		private boolean payIfGoalReached() {
			if (amount >= fundingGoal) {
				pay(beneficiary, amount);
				amount = 0;
				return true;
			}
			else
				return false;
		}
	}
}