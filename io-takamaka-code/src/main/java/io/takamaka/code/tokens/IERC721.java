package io.takamaka.code.tokens;

import java.math.BigInteger;

/*
Copyright 2021 Filippo Fantinato and Fausto Spoto

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

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.RequirementViolationException;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.UnsignedBigInteger;

/**
 * The interface of a contract for holding and transferring non-fungible tokens.
 */
public interface IERC721 extends IERC721View {

	/**
	 * Transfers token {@code tokenId} from {@code from} to {@code to}, checking first that contract recipients
	 * are aware of the ERC721 protocol to prevent tokens from being forever locked. Emits an {@link IERC721.Transfer} event.
	 * If the caller is not {@code from}, it must be have been allowed to move the token by either {@link #approve(Contract, BigInteger)}
	 * or {@link #setApprovalForAll(Contract, boolean)}. If {@code to} is a {@link IERC721Receiver}, its
	 * {@link IERC721Receiver#onERC721Received(Contract, Contract, BigInteger)} method gets invoked.
	 * This method was called {@code safeTransferFrom} in the original Solidity specification, while
	 * {@code transferFrom} was deprecated. This implementation decided to provide the non-deprecated
	 * version only, with the simpler name.
	 *
	 * @param from the original owner of the token. This cannot be {@code null} and must actually own the token
	 * @param to the new owner of the token. This must be non-{@code null} and either an externally owned account or
	 *           a contract that implements {#link {@link IERC721Receiver}
	 * @param tokenId the identifier of the token
	 */
	@FromContract
	void transferFrom(Contract from, Contract to, BigInteger tokenId);

	/**
	 * Gives permission to {@code to} to manage token {@code tokenId}.
	 * The approval is cleared when the token is transferred.
	 * Only a single account can be approved at a time, so using {@code null} for {@code to}
	 * clears previous approvals. Emits an {@link IERC721.Approval} event.
	 * The caller must own the token or be itself approved for the token.
	 *
	 * @param to the contract that receives the permission to transfer token {@code tokenId}.
	 *           This can be {@code null}, in which case the approval for {@code tokenId} gets cleared
	 * @param tokenId the identifier of the token
	 */
	@FromContract
	void approve(Contract to, BigInteger tokenId);

	/**
	 * Approves or removes {@code operator} as an operator for the caller.
	 * Operators can manage any token owned by the caller.
	 * Emits an {@link IERC721.ApprovalForAll} event.
	 *
	 * @param operator the contract that receives or loses the approval; this
	 *                 cannot be the caller of this method
	 * @param approved true if the operator must be approved, false if it must be removed
	 */
	@FromContract
	void setApprovalForAll(Contract operator, boolean approved);

	/**
	 * Yields the contract that has been approved for {@code tokenId}.
	 * 
	 * @param tokenId the index of the token
	 * @return the account approved for (@code tokenId). Yields {@code null} if
	 *         no account has been approved for {@code tokenId}
	 * @throws RequirementViolationException if the token is unknown
	 */
	@View
	Contract getApproved(BigInteger tokenId);

	/**
	 * Checks if all tokens of the given {@code owner} have been approved to be managed
	 * by the given {@code operator}.
	 * 
	 * @param owner the owner of the tokens
	 * @param operator the account that should be approved for all tokens of the {@code owner}
	 * @return true if and only if {@code operator} is allowed to manage all tokens of {@code owner}
	 */
	@View
	boolean isApprovedForAll(Contract owner, Contract operator);

	/**
	 * Emitted when the {@code tokenId} token is transferred from {@code from} to {@code to}.
	 */
	class Transfer extends Event {
		public final Contract from;
		public final Contract to;
		public final BigInteger tokenId;

		@FromContract
		public Transfer(Contract from, Contract to, BigInteger tokenId) {
			this.from = from;
			this.to = to;
			this.tokenId = tokenId;
		}
	}

	/**
	 * Emitted when {@code owner} enables {@code approved} to manage the {@code tokenId} token.
	 */
	class Approval extends Event {
		public final Contract owner;
		public final Contract approved;
		public final BigInteger tokenId;

		@FromContract
		public Approval(Contract owner, Contract approved, BigInteger tokenId) {
			this.owner = owner;
			this.approved = approved;
			this.tokenId = tokenId;
		}
	}

	/**
	 * Emitted when {@code owner} enables or disables {@code operator} to manage all its tokens.
	 */
	class ApprovalForAll extends Event {
		public final Contract owner;
		public final Contract operator;

		/**
		 * True when has been enabled. False when it has been disabled.
		 */
		public final boolean approved;

		@FromContract
		public ApprovalForAll(Contract owner, Contract operator, boolean approved) {
			this.owner = owner;
			this.operator = operator;
			this.approved = approved;
		}
	}
}