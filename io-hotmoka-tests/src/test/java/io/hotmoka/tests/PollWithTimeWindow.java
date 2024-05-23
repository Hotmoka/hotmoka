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

import static io.hotmoka.node.StorageTypes.BIG_INTEGER;
import static io.hotmoka.node.StorageTypes.PAYABLE_CONTRACT;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.StorageReference;

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

	private static final VoidMethodSignature VOTE_POLL = MethodSignatures.ofVoid(POLL_WITH_TIME_WINDOW, "vote");
	private static final VoidMethodSignature CLOSE_POLL = MethodSignatures.ofVoid(POLL_WITH_TIME_WINDOW, "close");
	private static final NonVoidMethodSignature IS_OVER = MethodSignatures.ofNonVoid(POLL_WITH_TIME_WINDOW, "isOver", StorageTypes.BOOLEAN);
	private static final NonVoidMethodSignature IS_RUN_PERFORMED = MethodSignatures.ofNonVoid(ACTION, "isRunPerformed", StorageTypes.BOOLEAN);

	private StorageReference stakeholder0;
	private StorageReference stakeholder1;
	private StorageReference stakeholder2;
	private StorageReference stakeholder3;

	private final static Logger LOGGER = Logger.getLogger(PollWithTimeWindow.class.getName());

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

	private static void sleep(long ms) throws InterruptedException {
		LOGGER.info("sleeping for " + ms + "ms");
		TimeUnit.MILLISECONDS.sleep(ms);
		LOGGER.info("waking up");
	}

	private StorageReference addSimpleSharedEntity(BigInteger share0, BigInteger share1, BigInteger share2, BigInteger share3) 
			throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _1_000_000, ONE, jar(),
			SIMPLE_SHARED_ENTITY_CONSTRUCTOR, stakeholder0, stakeholder1, stakeholder2, stakeholder3,
			StorageValues.bigIntegerOf(share0), StorageValues.bigIntegerOf(share1), StorageValues.bigIntegerOf(share2), StorageValues.bigIntegerOf(share3));
	}

	private StorageReference addPollWithTimeWindow(StorageReference sharedEntity, StorageReference action, long start, long duration) throws InvalidKeyException, SignatureException, TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _1_000_000, ONE, jar(), POLL_WITH_TIME_WINDOW_CONSTRUCTOR, sharedEntity, action, StorageValues.longOf(start), StorageValues.longOf(duration));
	}

	private StorageReference addAction() throws InvalidKeyException, SignatureException, TransactionException, CodeExecutionException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _1_000_000, ONE, jar(), ACTION_CONSTRUCTOR);
	}

	@Test
	@DisplayName("new PollWithTimeWindow() where time window is valid and all the 4 participants (having the same voting power) vote with their maximum voting power")
	void successfulPollWithValidWindowWhereAllStakeHoldersVote() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, 0L, 10_000L);
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		
		BooleanValue isOver = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertTrue(isOver.getValue());
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertTrue(isActionPerformed.getValue());
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() with close attempts before the window expired")
	void pollWithCloseAttemptsBeforeWindowExpired() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException, NodeException, TimeoutException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		// Tendermint is slower
		boolean isUsingTendermint = node.getNodeInfo().getType().contains("TendermintNode");
		long start = isUsingTendermint ? 10_000L : 2000L;
		long duration = isUsingTendermint ? 10_000L : 5000L;
		long expired = start + duration + 100L;
		long now = System.currentTimeMillis();
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);

		BooleanValue isOver = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertFalse(isOver.getValue());
		
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll));

		sleep(start - (System.currentTimeMillis() - now) + 2000);
		
		isOver = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_OVER, poll);
		Assertions.assertFalse(isOver.getValue());
		
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll));

		sleep(expired - (System.currentTimeMillis() - now) + 2000);
		
		isOver = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertTrue(isOver.getValue());
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.getValue());
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where time window is valid and all the 4 participants (having the same voting power) vote before and after start time with their maximum voting power")
	void successfulPollWithValidWindowWhereAllStakeHoldersVoteBeforeAndAfterStartTime() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException, NodeException, TimeoutException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		// the Tendermint blockchain is slower
		boolean isUsingTendermint = node.getNodeInfo().getType().contains("TendermintNode");
		long start = isUsingTendermint ? 10_000L : 2000L;
		long duration = isUsingTendermint ? 10_000L : 3000L;
		long now = System.currentTimeMillis();
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);

		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		
		sleep(start - (System.currentTimeMillis() - now) + 2000L);
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		
		var isOver = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertTrue(isOver.getValue());
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		var isActionPerformed = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertTrue(isActionPerformed.getValue());
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where a participant votes when the time window is expired")
	void voteWithExpiredTimeWindow() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException, NodeException, TimeoutException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		long start = 200L;
		long duration = 200L;
		long expired = start + duration + 2000L;
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);

		sleep(expired);
		
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll));

		BooleanValue isOver = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertTrue(isOver.getValue());
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.getValue());
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where one of the participants, holding a huge amount of voting power (more than 50% of the total) does not vote and the time window expired")
	void weightVoteWithExpiredTimeWindow() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException, NodeException, TimeoutException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		long start = 0L;
		// the Tendermint node is slower
		boolean isUsingTendermint = node.getNodeInfo().getType().contains("TendermintNode");
		long duration = isUsingTendermint ? 10_000L : 2000L;
		long expired = start + duration;
		long now = System.currentTimeMillis();
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);
		
		addInstanceVoidMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll);

		sleep(expired - (System.currentTimeMillis() - now) + 2000L);
		
		BooleanValue isOver = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);	
		Assertions.assertTrue(isOver.getValue());
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.getValue());
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() with time parameters which leads to a numerical overflow")
	void pollWithTimeWindowNumericalOverflow() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		
		throwsTransactionExceptionWithCauseAndMessageContaining(ArithmeticException.class, "long overflow",
			() -> addPollWithTimeWindow(simpleSharedEntity, action, Long.MAX_VALUE, 1L));
		throwsTransactionExceptionWithCauseAndMessageContaining(ArithmeticException.class, "long overflow",
			() -> addPollWithTimeWindow(simpleSharedEntity, action, 1L, Long.MAX_VALUE));
	}

	@Test
	@DisplayName("new PollWithTimeWindow() with negative time parameters")
	void failureToCreatePollWithNegativeParameters() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
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