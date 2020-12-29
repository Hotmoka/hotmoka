package io.takamaka.tests.crowdfunding;
import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageLinkedList;

public class CrowdFunding extends Contract {
	private final StorageList<Campaign> campaigns = new StorageLinkedList<>();

	public int newCampaign(PayableContract beneficiary, BigInteger goal) {
		campaigns.add(new Campaign(beneficiary, goal));
		return campaigns.size() - 1;
	}

	public @Payable @FromContract void contribute(BigInteger amount, int campaignID) {
		Campaign campaign = campaigns.get(campaignID);
		campaign.funders.add(new Funder(caller(), amount));
		campaign.amount = campaign.amount.add(amount);
	}

	public boolean checkGoalReached(int campaignID) {
		Campaign campaign = campaigns.get(campaignID);
		if (campaign.amount.compareTo(campaign.fundingGoal) < 0)
			return false;
		else {
			BigInteger amount = campaign.amount;
			campaign.amount = BigInteger.ZERO;
			campaign.beneficiary.receive(amount);
			return true;
		}
	}

	private static class Campaign extends Storage {
		private final PayableContract beneficiary;
		private final BigInteger fundingGoal;
		private final StorageList<Funder> funders = new StorageLinkedList<>();
		private BigInteger amount;

		private Campaign(PayableContract beneficiary, BigInteger fundingGoal) {
			this.beneficiary = beneficiary;
			this.fundingGoal = fundingGoal;
			this.amount = BigInteger.ZERO;
		}
	}

	private static class Funder extends Storage {
		@SuppressWarnings("unused")
		private final Contract who;
		@SuppressWarnings("unused")
		private final BigInteger amount;

		public Funder(Contract who, BigInteger amount) {
			this.who = who;
			this.amount = amount;
		}
	}
}