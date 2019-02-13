import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.Storage;
import takamaka.util.StorageList;

public class CrowdFunding extends Contract {
	private final StorageList<Campaign> campaigns = new StorageList<>();

	public void newCampaign(Contract beneficiary, int goal) {
		campaigns.add(new Campaign(beneficiary, goal));
	}

	public @Payable @Entry void contribute(int amount, int campaignID) {
		campaigns.elementAt(campaignID).addFunder(caller(), amount);
	}

	public boolean checkGoalReached(int campaignID) {
		return campaigns.elementAt(campaignID).payIfGoalReached();
	}

	private class Campaign extends Storage {
		private final Contract beneficiary;
		private final int fundingGoal;
		private final StorageList<Funder> funders = new StorageList<>();
		private int amount;

		private Campaign(Contract beneficiary, int fundingGoal) {
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