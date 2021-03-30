package io.hotmoka.tests;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import static io.hotmoka.beans.Coin.*;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static io.hotmoka.beans.types.BasicTypes.LONG;
import static io.hotmoka.beans.types.ClassType.SHARED_ENTITY;
import static io.hotmoka.beans.types.ClassType.BIG_INTEGER;
import static io.hotmoka.beans.types.ClassType.PAYABLE_CONTRACT;

/**
 * A test for the shared entity contract and subclasses.
 */
class SharedEntity extends TakamakaTest {
    private static final ClassType SIMPLE_SHARED_ENTITY = new ClassType("io.takamaka.code.dao.SimpleSharedEntity");
    private static final ClassType SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS = new ClassType(SHARED_ENTITY.name + "WithCappedShareholders");
    private static final ClassType SHARED_ENTITY_WITH_INTEGRAL_SHARES = new ClassType(SHARED_ENTITY.name + "WithIntegralShares");
    private static final ClassType SHARED_ENTITY_WITH_CAPPED_SHARES = new ClassType(SHARED_ENTITY.name + "WithCappedShares");
    private static final ClassType OFFER = new ClassType(SHARED_ENTITY.name + "$Offer");
    private static final ConstructorSignature SIMPLE_SHARED_ENTITY_CONSTRUCTOR = new ConstructorSignature(SIMPLE_SHARED_ENTITY, PAYABLE_CONTRACT, BIG_INTEGER);
    private static final ConstructorSignature SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS_CONSTRUCTOR = new ConstructorSignature(SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS, PAYABLE_CONTRACT, BIG_INTEGER, INT);
    private static final ConstructorSignature SHARED_ENTITY_WITH_INTEGRAL_SHARES_CONSTRUCTOR = new ConstructorSignature(SHARED_ENTITY_WITH_INTEGRAL_SHARES, PAYABLE_CONTRACT, BIG_INTEGER);
    private static final ConstructorSignature SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR = new ConstructorSignature(SHARED_ENTITY_WITH_CAPPED_SHARES, PAYABLE_CONTRACT, BIG_INTEGER, INT);
    private static final ConstructorSignature SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR_2 = new ConstructorSignature(SHARED_ENTITY_WITH_CAPPED_SHARES, PAYABLE_CONTRACT, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, INT);
    private static final ConstructorSignature OFFER_CONSTRUCTOR = new ConstructorSignature(OFFER, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, LONG);
    private static final BigInteger _500_000 = BigInteger.valueOf(500_000);
    private StorageReference creator;
    private StorageReference seller;
    private StorageReference buyer;
    private TransactionReference classpath_takamaka_code;

    @BeforeEach
    void beforeEach() throws Exception {
        setAccounts(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        creator = account(0);
        seller = account(1);
        buyer = account(2);
        classpath_takamaka_code = takamakaCode();
    }

    @Test
    @DisplayName("a seller cannot sell more shares than it owns")
    void cannotSellMoreSharesThanOwned() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create the shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SIMPLE_SHARED_ENTITY_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO));

        // create an offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // invalid: the seller is trying to sell more shares than it owns
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "the seller has not enough shares to sell", () ->
                addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY, "place", BIG_INTEGER, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer)
        );
    }

    @Test
    @DisplayName("a contract cannot place an offer on behalf of somebody else")
    void placeOnBehalfOfAnotherIsRejected() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SIMPLE_SHARED_ENTITY_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO));

        // create an offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // invalid: the creator is trying to place the offer on behalf of the seller
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "only the seller can place its own offer", () ->
                addInstanceMethodCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY, "place", BIG_INTEGER, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer)
        );
    }

    @Test
    @DisplayName("a contract can place its own offer")
    void placeOnBehalfOfOneselfWorks() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SIMPLE_SHARED_ENTITY_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO));

        // create an offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // valid: the seller places the second offer
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY, "place", BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);
    }

    @Test
    @DisplayName("acceptance with too little money is rejected")
    void acceptanceWithTooLittleMoneyIsRejected() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SIMPLE_SHARED_ENTITY_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN));

        // create an offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code, OFFER_CONSTRUCTOR,
                seller, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // the seller places his offer
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY, "place", BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

        // invalid: the buyer provides too little money
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "not enough money to accept the offer", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.ZERO), buyer, offer)
        );
    }

    @Test
    @DisplayName("acceptance with enough money works")
    void acceptanceWithEnoughMoneyWorks() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SIMPLE_SHARED_ENTITY_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN));

        // create an offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code, OFFER_CONSTRUCTOR,
                seller, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // the seller places his offer
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY, "place", BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

        // valid: the buyer provides enough money to accept the offer
        addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.TWO), buyer, offer);
    }

    @Test
    @DisplayName("more shareholders than the capped shareholders limit are rejected at initialization")
    void maxShareholdersLimitViolatedAtInitialization() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // invalid: limit reached in initialization
    	throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "too many shareholders", () ->
                addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                        SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(0))
        );
    }

    @Test
    @DisplayName("fewer shareholders than the capped shareholders limit are accepted at initialization")
    void maxShareholdersLimitRespectedAtInitialization() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // valid
        addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(1));
    }

    @Test
    @DisplayName("capped shareholders limit works when accepting an offer that crosses the cap")
    void maxShareholdersLimitViolatedAtAcceptance() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(1));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // the seller places the offer
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS, "place", BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

        // invalid: the maximal limit of shareholders has been reached
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "too many shareholders, the limit is 1", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.TEN), buyer, offer)
        );
    }

    @Test
    @DisplayName("capped shareholders limit works when accepting an offer that does not cross the cap")
    void maxShareholdersLimitRespectedAtAcceptance() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(2));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS, "place", BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

        // valid
        addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.TEN), buyer, offer);
    }

    @Test
    @DisplayName("an attempt to sell only a part of the shares fails")
    void attemptToSellOnlyPartOfSharesFails() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_INTEGRAL_SHARES_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // invalid: the seller tries to sell only 2 of its 10 shares
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "the seller must sell its shares in full", () ->
                addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY_WITH_INTEGRAL_SHARES, "place", BIG_INTEGER, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer)
        );
    }

    @Test
    @DisplayName("an attempt to sell all own shares succeeds")
    void attemptToSellAllSharesSucceeds() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_INTEGRAL_SHARES_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // valid: the seller sells all its 10 shares
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_WITH_INTEGRAL_SHARES, "place", BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

    }

    @Test
    @DisplayName("the maximal percent of shares is respected at initialization")
    void shareLimitReachedAtInitialization() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(100));

        // invalid: the maximal limit of shares is reached at initialization
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "a shareholder cannot hold more than", () ->
                addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(50))
        );
    }

    @Test
    @DisplayName("the maximal percent of shares cannot be smaller than one")
    void shareLimitIsSmallerThan1() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(100));

        // invalid: the limit percent of shares is not positive
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "invalid share limit", () ->
                addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(0))
        );
    }

    @Test
    @DisplayName("the maximal percent of shares cannot be larger than 100")
    void shareLimitIsLargerThan100() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(100));

        // invalid: the limit percent of shares is larger than 100
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "invalid share limit", () ->
                addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(101))
        );
    }

    @Test
    @DisplayName("the maximal percent of shares is respected when accepting an offer that stays inside the limit")
    void sharePercentLimitNotReached() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
    	// create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR_2, seller, buyer, new BigIntegerValue(BigInteger.TEN), new BigIntegerValue(BigInteger.TEN), new IntValue(70));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // place the offer on the shared entity
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_WITH_CAPPED_SHARES, "place", BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

        // valid
        addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_WITH_CAPPED_SHARES, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.TWO), buyer, offer);
    }

    @Test
    @DisplayName("the maximal percent of shares is respected when accepting an offer that goes beyond the limit")
    void sharePercentLimitReached() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR_2, seller, buyer, new BigIntegerValue(BigInteger.TEN), new BigIntegerValue(BigInteger.TEN), new IntValue(50));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // place the offer on the shared entity
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_WITH_CAPPED_SHARES, "place", BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

        // invalid: the share limit has been reached
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "a shareholder cannot hold more than", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY_WITH_CAPPED_SHARES, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.TWO), buyer, offer)
        );
    }
}