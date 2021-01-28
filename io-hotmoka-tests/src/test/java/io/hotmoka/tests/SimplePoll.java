package io.hotmoka.tests;

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
import io.hotmoka.beans.values.StorageReference;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimplePoll extends TakamakaTest {

	private static final BigInteger _20_000_000 = BigInteger.valueOf(20000000L);
	private static final BigInteger _10_000_000 = BigInteger.valueOf(10000000L);

	private static final ClassType SIMPLE_SHARED_ENTITY = new ClassType("io.takamaka.code.dao.SimpleSharedEntity");
	private static final ClassType SIMPLE_POLL = new ClassType("io.takamaka.code.dao.SimplePoll");
	private static final ClassType ACTION_SIMPLE_POLL = new ClassType("io.takamaka.code.dao.SimplePoll$Action");
	private static final ClassType ACTION = new ClassType("io.takamaka.code.dao.action.CheckRunPerformedAction");

	private static final ConstructorSignature SIMPLE_SHARED_ENTITY_CONSTRUCTOR = new ConstructorSignature( SIMPLE_SHARED_ENTITY, 
			ClassType.PAYABLE_CONTRACT, ClassType.PAYABLE_CONTRACT, ClassType.PAYABLE_CONTRACT, ClassType.PAYABLE_CONTRACT, 
			ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER);

	private static final ConstructorSignature SIMPLE_POLL_ENTITY_CONSTRUCTOR = new ConstructorSignature(SIMPLE_POLL, ClassType.SHARED_ENTITY, ACTION_SIMPLE_POLL);
	private static final ConstructorSignature ACTION_CONSTRUCTOR = new ConstructorSignature(ACTION);

	private static final VoidMethodSignature VOTE_POLL = new VoidMethodSignature(SIMPLE_POLL, "vote");
	private static final VoidMethodSignature VOTE_POLL_WITH_PARAM = new VoidMethodSignature(SIMPLE_POLL, "vote", ClassType.BIG_INTEGER);
	private static final VoidMethodSignature CLOSE_POLL = new VoidMethodSignature(SIMPLE_POLL, "close");

	private static final NonVoidMethodSignature IS_POLL_OVER = new NonVoidMethodSignature(SIMPLE_POLL, "isOver", BasicTypes.BOOLEAN);
	private static final NonVoidMethodSignature IS__RUN_ACTION_PERFORMED= new NonVoidMethodSignature(ACTION, "isRunPerformed", BasicTypes.BOOLEAN);

	private StorageReference stakeholder0;
	private StorageReference stakeholder1;
	private StorageReference stakeholder2;
	private StorageReference stakeholder3;

	@BeforeEach
	void beforeEach() throws Exception {
		
		setNode("basicdependency.jar", _20_000_000, _20_000_000, _20_000_000, _20_000_000);
		
		stakeholder0 = account(0);
		stakeholder1 = account(1);
		stakeholder2 = account(2);
		stakeholder3 = account(3);
	}

	StorageReference addSimpleSharedEntity(BigInteger share0, BigInteger share1, BigInteger share2, BigInteger share3) 
			throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ONE, jar(),
				SIMPLE_SHARED_ENTITY_CONSTRUCTOR, stakeholder0, stakeholder1, stakeholder2, stakeholder3,
						new BigIntegerValue(share0), new BigIntegerValue(share1), new BigIntegerValue(share2), new BigIntegerValue(share3));
	}

	StorageReference addSimplePoll(StorageReference sharedEntity, StorageReference action) throws InvalidKeyException, SignatureException, TransactionException, CodeExecutionException, TransactionRejectedException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ONE, jar(), SIMPLE_POLL_ENTITY_CONSTRUCTOR, sharedEntity, action);
	}

	StorageReference addAction() throws InvalidKeyException, SignatureException, TransactionException, CodeExecutionException, TransactionRejectedException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ONE, jar(), ACTION_CONSTRUCTOR);
	}

	@Test
	@DisplayName("NotOverPoll")
	void NotOverPoll() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), IS_POLL_OVER, simplePoll);
		
		Assertions.assertFalse(isOver.value);
	}

	@Test
	@DisplayName("AllStakeHolderVote")
	void IsOverPoll() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), IS_POLL_OVER, simplePoll);
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), CLOSE_POLL, simplePoll);
		
	}
	
	@Test
	@DisplayName("VotedMoreThan50Percent")
	void VotedMoreThan50Percent() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), CLOSE_POLL, simplePoll);
		
	}
	
	@Test
	@DisplayName("WeightVote")
	void WeightVote() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity( BigInteger.valueOf(10), BigInteger.ONE, BigInteger.ONE, BigInteger.ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), CLOSE_POLL, simplePoll);
	}
	
	@Test
	@DisplayName("VariableWeightVote")
	void VariableWeightVote() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity( BigInteger.valueOf(10), BigInteger.ONE, BigInteger.ONE, BigInteger.ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.valueOf(8)));
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), CLOSE_POLL, simplePoll);
	}
	
	@Test
	@DisplayName("ZeroWeightVote")
	void ZeroWeightVote() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity( BigInteger.valueOf(10), BigInteger.ONE, BigInteger.ONE, BigInteger.ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.valueOf(0)));
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.valueOf(0)));
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.valueOf(0)));
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.valueOf(0)));
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), CLOSE_POLL, simplePoll);
	}
	
	@Test
	@DisplayName("NotOverPollWithWeightVotes")
	void NotOverPollWithWeightVotes() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity( BigInteger.valueOf(10), BigInteger.ONE, BigInteger.ONE, BigInteger.ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _10_000_000, BigInteger.ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, BigInteger.ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertFalse(isOver.value);

	}
	
	
}
