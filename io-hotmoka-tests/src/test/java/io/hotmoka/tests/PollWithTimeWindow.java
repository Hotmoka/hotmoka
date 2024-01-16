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

package io.hotmoka.tests;

import static io.hotmoka.beans.StorageTypes.BIG_INTEGER;
import static io.hotmoka.beans.StorageTypes.PAYABLE_CONTRACT;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.signatures.MethodSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;

class PollWithTimeWindow extends HotmokaTest {
	private static final ClassType SIMPLE_SHARED_ENTITY = StorageTypes.classNamed("io.takamaka.code.dao.SimpleSharedEntity");
	private static final ClassType POLL_WITH_TIME_WINDOW = StorageTypes.classNamed("io.takamaka.code.dao.PollWithTimeWindow");
	private static final ClassType ACTION_SIMPLE_POLL = StorageTypes.classNamed("io.takamaka.code.dao.SimplePoll$Action");
	private static final ClassType ACTION = StorageTypes.classNamed("io.hotmoka.examples.polls.CheckRunPerformedAction");

	private static final ConstructorSignature SIMPLE_SHARED_ENTITY_CONSTRUCTOR = ConstructorSignatures.of(SIMPLE_SHARED_ENTITY, 
		PAYABLE_CONTRACT, PAYABLE_CONTRACT, PAYABLE_CONTRACT, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER);
	private static final ConstructorSignature POLL_WITH_TIME_WINDOW_CONSTRUCTOR = ConstructorSignatures.of
		(POLL_WITH_TIME_WINDOW, StorageTypes.SHARED_ENTITY_VIEW, ACTION_SIMPLE_POLL, StorageTypes.LONG, StorageTypes.LONG);
	private static final ConstructorSignature ACTION_CONSTRUCTOR = ConstructorSignatures.of(ACTION);

	private static final MethodSignature VOTE_POLL = MethodSignatures.ofVoid(POLL_WITH_TIME_WINDOW, "vote");
	private static final MethodSignature CLOSE_POLL = MethodSignatures.ofVoid(POLL_WITH_TIME_WINDOW, "close");
	private static final MethodSignature IS_OVER = MethodSignatures.of(POLL_WITH_TIME_WINDOW, "isOver", StorageTypes.BOOLEAN);
	private static final MethodSignature IS_RUN_PERFORMED = MethodSignatures.of(ACTION, "isRunPerformed", StorageTypes.BOOLEAN);

	private StorageReference stakeholder0;
	private StorageReference stakeholder1;
	private StorageReference stakeholder2;
	private StorageReference stakeholder3;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("polls.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, _1_000_000_000, _1_000_000_000, _1_000_000_000);
		
		stakeholder0 = account(0);
		stakeholder1 = account(1);
		stakeholder2 = account(2);
		stakeholder3 = account(3);
	}

	private StorageReference addSimpleSharedEntity(BigInteger share0, BigInteger share1, BigInteger share2, BigInteger share3) 
			throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _1_000_000, ONE, jar(),
			SIMPLE_SHARED_ENTITY_CONSTRUCTOR, stakeholder0, stakeholder1, stakeholder2, stakeholder3,
			StorageValues.bigIntegerOf(share0), StorageValues.bigIntegerOf(share1), StorageValues.bigIntegerOf(share2), StorageValues.bigIntegerOf(share3));
	}

	private StorageReference addPollWithTimeWindow(StorageReference sharedEntity, StorageReference action, long start, long duration) throws InvalidKeyException, SignatureException, TransactionException, CodeExecutionException, TransactionRejectedException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _1_000_000, ONE, jar(), POLL_WITH_TIME_WINDOW_CONSTRUCTOR, sharedEntity, action, StorageValues.longOf(start), StorageValues.longOf(duration));
	}

	private StorageReference addAction() throws InvalidKeyException, SignatureException, TransactionException, CodeExecutionException, TransactionRejectedException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _1_000_000, ONE, jar(), ACTION_CONSTRUCTOR);
	}

	@Test
	@DisplayName("new PollWithTimeWindow() where time window is valid and all the 4 participants (having the same voting power) vote with their maximum voting power")
	void successfulPollWithValidWindowWhereAllStakeHoldersVote() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, 0L, 10_000L);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		
		BooleanValue isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertTrue(isOver.getValue());
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertTrue(isActionPerformed.getValue());
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() with close attempts before the window expired")
	void pollWithCloseAttemptsBeforeWindowExpired() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		// Tendermint is slower
		long start = isUsingTendermint() || node instanceof RemoteNode ? 10_000L : 1000L;
		long duration = isUsingTendermint() || node instanceof RemoteNode ? 10_000L : 5000L;
		long expired = start + duration + 100L;
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);
		long now = System.currentTimeMillis();

		BooleanValue isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertFalse(isOver.getValue());
		
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll));

		TimeUnit.MILLISECONDS.sleep(start - (System.currentTimeMillis() - now) + 2000);
		
		isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_OVER, poll);
		Assertions.assertFalse(isOver.getValue());
		
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll));
		
		TimeUnit.MILLISECONDS.sleep(expired - (System.currentTimeMillis() - now) + 2000);
		
		isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertTrue(isOver.getValue());
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.getValue());
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where time window is valid and all the 4 participants (having the same voting power) vote before and after start time with their maximum voting power")
	void successfulPollWithValidWindowWhereAllStakeHoldersVoteBeforeAndAfterStartTime() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		// the Tendermint blockchain is slower
		long start = isUsingTendermint() || node instanceof RemoteNode ? 10_000L : 1000L;
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, 10_000L);
		long now = System.currentTimeMillis();

		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		
		TimeUnit.MILLISECONDS.sleep(start - (System.currentTimeMillis() - now) + 2000L);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		
		BooleanValue isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertTrue(isOver.getValue());
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertTrue(isActionPerformed.getValue());
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where a participant votes when the wime window is expired")
	void voteWithExpiredTimeWindow() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		long start = 200L;
		long duration = 200L;
		long expired = start + duration + 2000L;
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);
		
		TimeUnit.MILLISECONDS.sleep(expired);
		
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll));

		BooleanValue isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertTrue(isOver.getValue());
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.getValue());
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where one of the participants, holding a huge amount of voting power (more than 50% of the total) does not vote and the time window expired")
	void weightVoteWithExpiredTimeWindow() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		long start = 0L;
		// the Tendermint node is slower
		long duration = isUsingTendermint() || node instanceof RemoteNode ? 10_000L : 2000L;
		long expired = start + duration;
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);
		long now = System.currentTimeMillis();
		
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		
		TimeUnit.MILLISECONDS.sleep(expired - (System.currentTimeMillis() - now) + 2000);
		
		BooleanValue isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);	
		Assertions.assertTrue(isOver.getValue());
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.getValue());
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() with time parameters which leads to a numerical overflow")
	void pollWithTimeWindowNumericalOverflow() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		
		throwsTransactionExceptionWithCauseAndMessageContaining(ArithmeticException.class, "long overflow",
			() -> addPollWithTimeWindow(simpleSharedEntity, action, Long.MAX_VALUE, 1L));
		throwsTransactionExceptionWithCauseAndMessageContaining(ArithmeticException.class, "long overflow",
			() -> addPollWithTimeWindow(simpleSharedEntity, action, 1L, Long.MAX_VALUE));
	}

	@Test
	@DisplayName("new PollWithTimeWindow() with negative time parameters")
	void failureToCreatePollWithNegativeParameters() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();

		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "the time parameters cannot be negative", () ->
			addPollWithTimeWindow(simpleSharedEntity, action, 1L, -1L));
		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "the time parameters cannot be negative", () ->
			addPollWithTimeWindow(simpleSharedEntity, action, -1L, 1L));
		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "the time parameters cannot be negative", () ->
			addPollWithTimeWindow(simpleSharedEntity, action, -1L, -1L));
		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "the time parameters cannot be negative", () ->
			addPollWithTimeWindow(simpleSharedEntity, action, Long.MAX_VALUE + 1L, 1L));
		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "the time parameters cannot be negative", () ->
			addPollWithTimeWindow(simpleSharedEntity, action, 1L, Long.MAX_VALUE + 1L));
		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "the time parameters cannot be negative", () ->
			addPollWithTimeWindow(simpleSharedEntity, action, Long.MAX_VALUE + 1L, Long.MAX_VALUE + 1L));
	}
}