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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
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

	private StorageReference addSimpleSharedEntity(BigInteger share0, BigInteger share1, BigInteger share2, BigInteger share3) throws Exception {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _1_000_000, ONE, jar(),
			SIMPLE_SHARED_ENTITY_CONSTRUCTOR, stakeholder0, stakeholder1, stakeholder2, stakeholder3,
			StorageValues.bigIntegerOf(share0), StorageValues.bigIntegerOf(share1), StorageValues.bigIntegerOf(share2), StorageValues.bigIntegerOf(share3));
	}

	private StorageReference addPollWithTimeWindow(StorageReference sharedEntity, StorageReference action, long start, long duration) throws Exception {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _1_000_000, ONE, jar(), POLL_WITH_TIME_WINDOW_CONSTRUCTOR, sharedEntity, action, StorageValues.longOf(start), StorageValues.longOf(duration));
	}

	private StorageReference addAction() throws Exception {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _1_000_000, ONE, jar(), ACTION_CONSTRUCTOR);
	}

	@Test
	@DisplayName("new PollWithTimeWindow() where time window is valid and all the 4 participants (having the same voting power) vote with their maximum voting power")
	void successfulPollWithValidWindowWhereAllStakeHoldersVote() throws Exception {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();

		// the Tendermint and Mokamint blockchains are slower
		String type = node.getInfo().getType();
		boolean isTendermint = type.contains("TendermintNode");
		boolean isMokamint = type.contains("MokamintNode");
		long duration = isMokamint ? 40_000 : isTendermint ? 10_000 : 3000;
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, 0L, duration);
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll);

		boolean isOver = runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll).asReturnedBoolean(IS_OVER, UnexpectedValueException::new);
		Assertions.assertTrue(isOver);
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		boolean isRunPerformed = runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action).asReturnedBoolean(IS_RUN_PERFORMED, UnexpectedValueException::new);
		Assertions.assertTrue(isRunPerformed);
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() with close attempts before the window expired")
	void pollWithCloseAttemptsBeforeWindowExpired() throws Exception {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		// the Tendermint and Mokamint blockchains are slower
		String type = node.getInfo().getType();
		boolean isTendermint = type.contains("TendermintNode");
		boolean isMokamint = type.contains("MokamintNode");
		long start = isMokamint ? 40_000 : isTendermint ? 10_000 : 2000;
		long duration = isMokamint ? 40_000 : isTendermint ? 10_000 : 3000;
		long expired = start + duration + (isMokamint ? 30_000 : 2_000);
		long now = System.currentTimeMillis();
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);

		boolean isOver = runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll).asReturnedBoolean(IS_OVER, UnexpectedValueException::new);
		Assertions.assertFalse(isOver);
		
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll));

		sleep(start - (System.currentTimeMillis() - now) + 2000);

		isOver = runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_OVER, poll).asReturnedBoolean(IS_OVER, UnexpectedValueException::new);
		Assertions.assertFalse(isOver);

		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll));

		sleep(expired - (System.currentTimeMillis() - now));

		isOver = runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll).asReturnedBoolean(IS_OVER, UnexpectedValueException::new);
		Assertions.assertTrue(isOver);

		// with Mokamint, it is possible that the previous run succeeds (the poll is over) but the next call fails (the poll is not over):
		// this is because run transactions are run in the current UTC time while add transactions are run in the time of creation of the block,
		// that in general is a bit before than the former; because of this, it is safer to add a large delay in "expired" (30 seconds)
		// in order to give time to Mokamint to create a new block, with an updated time
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		boolean isRunPerformed = runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action).asReturnedBoolean(IS_RUN_PERFORMED, UnexpectedValueException::new);
		Assertions.assertFalse(isRunPerformed);
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where time window is valid and all the 4 participants (having the same voting power) vote before and after start time with their maximum voting power")
	void successfulPollWithValidWindowWhereAllStakeHoldersVoteBeforeAndAfterStartTime() throws Exception {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		// the Tendermint and Mokamint blockchains are slower
		String type = node.getInfo().getType();
		boolean isTendermint = type.contains("TendermintNode");
		boolean isMokamint = type.contains("MokamintNode");
		long start = isMokamint ? 40_000 : isTendermint ? 10_000 : 2000;
		long duration = isMokamint ? 40_000 : isTendermint ? 10_000 : 3000;
		long now = System.currentTimeMillis();
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);

		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll));
		
		sleep(start - (System.currentTimeMillis() - now) + (isMokamint ? 10_000 : 2_000));
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll);

		boolean isOver = runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll).asReturnedBoolean(IS_OVER, UnexpectedValueException::new);
		Assertions.assertTrue(isOver);

		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		boolean isRunPerformed = runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action).asReturnedBoolean(IS_RUN_PERFORMED, UnexpectedValueException::new);
		Assertions.assertTrue(isRunPerformed);
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where a participant votes when the time window is expired")
	void voteWithExpiredTimeWindow() throws Exception {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		long start = 200L;
		long duration = 200L;
		// the Mokamint blockchain is slower
		String type = node.getInfo().getType();
		boolean isMokamint = type.contains("MokamintNode");
		long expired = start + duration + (isMokamint ? 20_000 : 2_000);
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);

		sleep(expired);
		
		assertThrows(TransactionException.class, () -> addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), VOTE_POLL, poll));

		boolean isOver = runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll).asReturnedBoolean(IS_OVER, UnexpectedValueException::new);
		Assertions.assertTrue(isOver);
		
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		boolean isRunPerformed = runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action).asReturnedBoolean(IS_RUN_PERFORMED, UnexpectedValueException::new);
		Assertions.assertFalse(isRunPerformed);
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where one of the participants, holding a huge amount of voting power (more than 50% of the total) does not vote and the time window expired")
	void weightVoteWithExpiredTimeWindow() throws Exception {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		long start = 0;
		// the Tendermint and Mokamint blockchains are slower
		String type = node.getInfo().getType();
		boolean isTendermint = type.contains("TendermintNode");
		boolean isMokamint = type.contains("MokamintNode");
		long duration = isMokamint ? 40_000 : isTendermint ? 10_000 : 3000;
		long expired = start + duration + (isMokamint ? 30_000 : 2000);
		long now = System.currentTimeMillis();
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);

		addInstanceVoidMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceVoidMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll);

		sleep(expired - (System.currentTimeMillis() - now));
		
		boolean isOver = runInstanceNonVoidMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll).asReturnedBoolean(IS_OVER, UnexpectedValueException::new);
		Assertions.assertTrue(isOver);

		// with Mokamint, it is possible that the previous run succeeds (the poll is over) but the next call fails (the poll is not over):
		// this is because run transactions are run in the current UTC time while add transactions are run in the time of creation of the block,
		// that in general is a bit before than the former; because of this, it is safer to add a large delay in "expired" (30 seconds)
		// in order to give time to Mokamint to create a new block, with an updated time
		addInstanceVoidMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);

		boolean isRunPerformed = runInstanceNonVoidMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action).asReturnedBoolean(IS_RUN_PERFORMED, UnexpectedValueException::new);
		Assertions.assertFalse(isRunPerformed);
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() with time parameters that lead to a numerical overflow")
	void pollWithTimeWindowNumericalOverflow() throws Exception {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		
		throwsTransactionExceptionWithCause(ArithmeticException.class, () -> addPollWithTimeWindow(simpleSharedEntity, action, Long.MAX_VALUE, 1L));
		throwsTransactionExceptionWithCause(ArithmeticException.class, () -> addPollWithTimeWindow(simpleSharedEntity, action, 1L, Long.MAX_VALUE));
	}

	@Test
	@DisplayName("new PollWithTimeWindow() with negative time parameters")
	void failureToCreatePollWithNegativeParameters() throws Exception {
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