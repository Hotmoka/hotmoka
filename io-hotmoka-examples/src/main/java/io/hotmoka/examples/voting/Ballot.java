/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.examples.voting;

import static io.takamaka.code.lang.Takamaka.require;

import java.util.HashSet;
import java.util.Set;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageListView;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

public class Ballot extends Contract {
	private final Contract chairperson;
	private final StorageMap<Contract, VotingPaper> papers = new StorageTreeMap<>();
	private final StorageList<Proposal> proposals = new StorageLinkedList<>();

	public @FromContract Ballot(StorageListView<String> proposalNames) {
		chairperson = caller();
		VotingPaper votingPaper = new VotingPaper();
		votingPaper.giveRightToVote(); // the chairperson has right to vote
		papers.put(chairperson, votingPaper);
		proposalNames.forEach(name -> proposals.add(new Proposal(name)));
	}

	public @FromContract void giveRightToVote(Contract to) {
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

	public @FromContract void delegate(Contract to) {
		VotingPaper delegatedPaper = papers.get(to);
		require(delegatedPaper.hasRightToVote(), "The delegated contract has no right to vote");
		getVotingPaperFor(caller()).delegateTo(caller(), to);
	}

	public @FromContract void voteFor(int proposalIndex) {
		getVotingPaperFor(caller()).voteFor(proposals.get(proposalIndex));
    }

	public String winnerName() {
		class WinnerName {
			private String name = "no winner";
			private int min = -1;
		}

		var wn = new WinnerName();

		proposals.forEach(proposal -> {
			if (wn.min == -1 || proposal.voteCount < wn.min) {
				wn.name = proposal.name;
				wn.min = proposal.voteCount;
			}
		});

		return wn.name;
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