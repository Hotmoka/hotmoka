import static java.util.Comparator.comparingInt;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Storage;
import takamaka.util.StorageList;
import takamaka.util.StorageMap;

public class Ballot extends Contract {
	private final Contract chairperson;
	private final StorageMap<Contract, VotingPaper> papers = new StorageMap<>(__ -> new VotingPaper());
	private final StorageList<Proposal> proposals = new StorageList<>();

	public @Entry Ballot(String[] proposalNames) {
		chairperson = caller();
		VotingPaper votingPaper = new VotingPaper();
		votingPaper.giveRightToVote(); // the chairperson has right to vote
		papers.put(chairperson, votingPaper);

		Stream.of(proposalNames)
			.map(Proposal::new)
			.forEach(proposals::add);
	}

	public @Entry void giveRightToVote(Contract to) {
		require(caller() == chairperson, "Only the chairperson can give right to vote");
		VotingPaper paper = papers.get(to);
		require(!paper.hasVoted(), "You already voted");
		require(!paper.hasRightToVote(), "You already have right to vote.");
		paper.giveRightToVote();
	}

	private VotingPaper getVotingPaper(Contract contract) {
		VotingPaper sender = papers.get(contract);
		require(!sender.hasVoted(), "You already voted");
		require(sender.hasRightToVote(), "You don't have the right to vote.");
		return sender;
	}

	public @Entry void delegate(Contract to) {
		VotingPaper delegatedPaper = papers.get(to);
		require(delegatedPaper.hasRightToVote(), "Your delegated has no right to vote.");
		getVotingPaper(caller()).delegateTo(caller(), to);
	}

	public @Entry void voteFor(int proposalIndex) {
		getVotingPaper(caller()).voteFor(proposalIndex);
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
		private boolean voted;
		private Contract delegate;
		private int vote;

		private VotingPaper() {}

		private boolean hasVoted() {
			return voted;
		}

		private boolean hasRightToVote() {
			return weight > 0;
		}

		private void giveRightToVote() {
			this.weight = 1;
		}

		private void delegateTo(Contract payer, Contract delegate) {
			delegate = computeFinalDelegate(payer, delegate);
			this.voted = true;
			this.delegate = delegate;

			VotingPaper paperOfDelegate = papers.get(delegate);
			if (paperOfDelegate.hasVoted())
				proposals.elementAt(paperOfDelegate.vote).voteCount += weight;
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

		private void voteFor(int proposalIndex) {
			voted = true;
			this.vote = proposalIndex;

			// If `vote` is out of the range of the array,
	        // this will throw automatically and revert all changes.
	        proposals.elementAt(proposalIndex).voteCount += weight;
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