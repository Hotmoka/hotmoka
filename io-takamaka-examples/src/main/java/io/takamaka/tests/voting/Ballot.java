package io.takamaka.tests.voting;

import static io.takamaka.code.lang.Takamaka.require;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageMap;

public class Ballot extends Contract {
	private final Contract chairperson;
	private final StorageMap<Contract, VotingPaper> papers = new StorageMap<>();
	private final StorageList<Proposal> proposals = new StorageList<>();

	public @Entry Ballot(StorageList<String> proposalNames) {
		chairperson = caller();
		VotingPaper votingPaper = new VotingPaper();
		votingPaper.giveRightToVote(); // the chairperson has right to vote
		papers.put(chairperson, votingPaper);
		proposalNames.stream().map(Proposal::new).forEachOrdered(proposals::add);
	}

	public @Entry void giveRightToVote(Contract to) {
		require(caller() == chairperson, "Only the chairperson can give right to vote");
		VotingPaper paper = papers.computeIfAbsent(to, VotingPaper::new);
		require(!paper.hasVoted(), "The contract already voted");
		require(!paper.hasRightToVote(), "The contract has already right to vote");
		paper.giveRightToVote();
	}

	private VotingPaper getVotingPaperFor(Contract contract) {
		VotingPaper paper = papers.get(contract);
		require(!paper.hasVoted(), "The contract already voted");
		require(paper.hasRightToVote(), "The contract has no right to vote");
		return paper;
	}

	public @Entry void delegate(Contract to) {
		VotingPaper delegatedPaper = papers.get(to);
		require(delegatedPaper.hasRightToVote(), "The delegated contract has no right to vote");
		getVotingPaperFor(caller()).delegateTo(caller(), to);
	}

	public @Entry void voteFor(int proposalIndex) {
		getVotingPaperFor(caller()).voteFor(proposals.get(proposalIndex));
    }

	public String winnerName() {
		return proposals.stream().min(Comparator.comparingInt(proposal -> proposal.voteCount)).get().name;
	}

	private class VotingPaper extends Storage {
		private int weight;
		private boolean hasVoted;
		private Contract delegate;
		private Proposal votedProposal;

		private VotingPaper() {}

		private boolean hasVoted() {
			return hasVoted;
		}

		private boolean hasRightToVote() {
			return weight > 0;
		}

		private void giveRightToVote() {
			this.weight = 1;
		}

		private void delegateTo(Contract payer, Contract delegate) {
			delegate = computeFinalDelegate(payer, delegate);
			this.hasVoted = true;
			this.delegate = delegate;

			VotingPaper paperOfDelegate = papers.get(delegate);
			if (paperOfDelegate.hasVoted())
				votedProposal.voteCount += weight;
			else
				paperOfDelegate.weight += weight;
		}

		private Contract computeFinalDelegate(Contract payer, Contract delegate) {
			require(delegate != payer, "Self-delegation is disallowed");

			Set<Contract> seen = new HashSet<>();
			VotingPaper from = papers.get(delegate);
			while (from.delegate != null) {
				delegate = from.delegate;
				require(delegate != payer, "Self-delegation is disallowed");
				require(seen.add(delegate), "Found loop in delegation");
				from = papers.get(delegate);
			}

			return delegate;
		}

		private void voteFor(Proposal proposal) {
			hasVoted = true;
			votedProposal = proposal;
			proposal.voteCount += weight;
		}
	}

	private static class Proposal extends Storage {
		private final String name;
		private int voteCount;

		public Proposal(String name) {
			this.name = name;
		}
	}
}