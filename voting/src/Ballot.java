import static java.util.Comparator.comparingInt;
import static takamaka.lang.Takamaka.require;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Storage;
import takamaka.util.StorageList;
import takamaka.util.StorageMap;

public class Ballot extends Contract {
	private final Contract chairperson;
	private final StorageMap<Contract, VotingPaper> papers = new StorageMap<>();
	private final StorageList<Proposal> proposals = new StorageList<>();

	public @Entry Ballot(StorageList<String> proposalNames) {
		chairperson = caller();
		VotingPaper votingPaper = new VotingPaper();
		votingPaper.giveRightToVote(); // the chairperson has right to vote
		papers.put(chairperson, votingPaper);
		proposalNames.forEach(name -> proposals.add(new Proposal(name)));
	}

	public @Entry void giveRightToVote(Contract to) {
		require(caller() == chairperson, "Only the chairperson can give right to vote");
		VotingPaper paper = papers.get(to, VotingPaper::new);
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
		getVotingPaperFor(caller()).voteFor(proposals.elementAt(proposalIndex));
    }

	private int indexOfWinningProposal() {
		return IntStream.range(0, proposals.size()).boxed()
				.min(comparingInt(index -> proposals.elementAt(index).voteCount))
				.get();  // or throw if empty list
	}

	public String winnerName() {
		return proposals.elementAt(indexOfWinningProposal()).name;
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