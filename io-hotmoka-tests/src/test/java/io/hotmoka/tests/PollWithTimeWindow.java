package io.hotmoka.tests;

import static io.hotmoka.beans.types.ClassType.BIG_INTEGER;
import static io.hotmoka.beans.types.ClassType.PAYABLE_CONTRACT;
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
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.remote.RemoteNode;

class PollWithTimeWindow extends TakamakaTest {
	private static final ClassType SIMPLE_SHARED_ENTITY = new ClassType("io.takamaka.code.dao.SimpleSharedEntity");
	private static final ClassType POLL_WITH_TIME_WINDOW = new ClassType("io.takamaka.code.dao.PollWithTimeWindow");
	private static final ClassType ACTION_SIMPLE_POLL = new ClassType("io.takamaka.code.dao.SimplePoll$Action");
	private static final ClassType ACTION = new ClassType("io.hotmoka.examples.polls.CheckRunPerformedAction");

	private static final ConstructorSignature SIMPLE_SHARED_ENTITY_CONSTRUCTOR = new ConstructorSignature(SIMPLE_SHARED_ENTITY, 
		PAYABLE_CONTRACT, PAYABLE_CONTRACT, PAYABLE_CONTRACT, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER);
	private static final ConstructorSignature POLL_WITH_TIME_WINDOW_CONSTRUCTOR = new ConstructorSignature
		(POLL_WITH_TIME_WINDOW, ClassType.SHARED_ENTITY_VIEW, ACTION_SIMPLE_POLL, BasicTypes.LONG, BasicTypes.LONG);
	private static final ConstructorSignature ACTION_CONSTRUCTOR = new ConstructorSignature(ACTION);

	private static final VoidMethodSignature VOTE_POLL = new VoidMethodSignature(POLL_WITH_TIME_WINDOW, "vote");
	private static final VoidMethodSignature CLOSE_POLL = new VoidMethodSignature(POLL_WITH_TIME_WINDOW, "close");

	private static final NonVoidMethodSignature IS_OVER = new NonVoidMethodSignature(POLL_WITH_TIME_WINDOW, "isOver", BasicTypes.BOOLEAN);
	private static final NonVoidMethodSignature IS_RUN_PERFORMED= new NonVoidMethodSignature(ACTION, "isRunPerformed", BasicTypes.BOOLEAN);

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
			new BigIntegerValue(share0), new BigIntegerValue(share1), new BigIntegerValue(share2), new BigIntegerValue(share3));
	}

	private StorageReference addPollWithTimeWindow(StorageReference sharedEntity, StorageReference action, long start, long duration) throws InvalidKeyException, SignatureException, TransactionException, CodeExecutionException, TransactionRejectedException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _1_000_000, ONE, jar(), POLL_WITH_TIME_WINDOW_CONSTRUCTOR, sharedEntity, action, new LongValue(start), new LongValue(duration));
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
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertTrue(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() with close attempts before the window expired")
	void pollWithCloseAttemptsBeforeWindowExpired() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		// Tendermint is slower
		long start = tendermintBlockchain != null || node instanceof RemoteNode ? 10_000L : 1000L;
		long duration = tendermintBlockchain != null || node instanceof RemoteNode ? 10_000L : 5000L;
		long expired = start + duration + 100L;
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);
		long now = System.currentTimeMillis();

		BooleanValue isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertFalse(isOver.value);
		
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll));

		TimeUnit.MILLISECONDS.sleep(start - (System.currentTimeMillis() - now) + 2000);
		
		isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_OVER, poll);
		Assertions.assertFalse(isOver.value);
		
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll));
		
		TimeUnit.MILLISECONDS.sleep(expired - (System.currentTimeMillis() - now) + 2000);
		
		isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where time window is valid and all the 4 participants (having the same voting power) vote before and after start time with their maximum voting power")
	void successfulPollWithValidWindowWhereAllStakeHoldersVoteBeforeAndAfterStartTime() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		// the Tendermint blockchain is slower
		long start = tendermintBlockchain != null || node instanceof RemoteNode ? 10_000L : 1000L;
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
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertTrue(isActionPerformed.value);
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
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() where one of the participants, holding a huge amount of voting power (more than 50% of the total) does not vote and the time window expired")
	void weightVoteWithExpiredTimeWindow() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		long start = 0L;
		// the Tendermint blockchain is slower
		long duration = tendermintBlockchain != null || node instanceof RemoteNode ? 10_000L : 2000L;
		long expired = start + duration;
		StorageReference poll = addPollWithTimeWindow(simpleSharedEntity, action, start, duration);
		long now = System.currentTimeMillis();
		
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _1_000_000, ZERO, jar(), VOTE_POLL, poll);
		
		TimeUnit.MILLISECONDS.sleep(expired - (System.currentTimeMillis() - now) + 2000);
		
		BooleanValue isOver = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _100_000, jar(), IS_OVER, poll);	
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _1_000_000, ZERO, jar(), CLOSE_POLL, poll);
		
		BooleanValue isActionPerformed = (BooleanValue) runInstanceMethodCallTransaction(stakeholder0, _50_000, jar(), IS_RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new PollWithTimeWindow() with time parameters which leads to a numerical overflow")
	void pollWithTimeWindowNumericalOverflow() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException {
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		
		throwsTransactionExceptionWithCauseAndMessageContaining(ArithmeticException.class, "long overflow",
			() -> addPollWithTimeWindow(simpleSharedEntity, action, Long.MAX_VALUE, 1L));
		throwsTransactionExceptionWithCauseAndMessageContaining(ArithmeticException.class, "long overflow",
			() -> addPollWithTimeWindow(simpleSharedEntity, action, 1L, Long.MAX_VALUE));
	}

	@Test
	@DisplayName("new PollWithTimeWindow() with negative time parameters")
	void failureToCreatePollWithNegativeParameters() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException {
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