package io.takamaka.code.tests;

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

/**
 * A test for the shared entity contract and subclasses.
 */
class SharedEntity extends TakamakaTest {
    private static final ClassType SHARED_ENTITY = new ClassType("io.takamaka.code.dao.SharedEntity");
    private static final ClassType SHARED_ENTITY_MAX_SHAREHOLDERS = new ClassType(SHARED_ENTITY + "MaxShareholders");
    private static final ClassType SHARED_ENTITY_SELL_ALL_SHARES = new ClassType(SHARED_ENTITY + "SellAllShares");
    private static final ClassType SHARED_ENTITY_SHARE_LIMIT = new ClassType(SHARED_ENTITY + "ShareLimit");
    private static final ClassType OFFER = new ClassType(SHARED_ENTITY + "$Offer");
    private static final ConstructorSignature SHARED_ENTITY_CONSTRUCTOR = new ConstructorSignature(SHARED_ENTITY, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER);
    private static final ConstructorSignature SHARED_ENTITY_MAX_SHAREHOLDERS_CONSTRUCTOR = new ConstructorSignature(SHARED_ENTITY_MAX_SHAREHOLDERS, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER, INT);
    private static final ConstructorSignature SHARED_ENTITY_SELL_ALL_SHARES_CONSTRUCTOR = new ConstructorSignature(SHARED_ENTITY_SELL_ALL_SHARES, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER);
    private static final ConstructorSignature SHARED_ENTITY_SHARE_LIMIT_CONSTRUCTOR = new ConstructorSignature(SHARED_ENTITY_SHARE_LIMIT, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER, INT);
    private static final ConstructorSignature SHARED_ENTITY_SHARE_LIMIT_CONSTRUCTOR_2 = new ConstructorSignature(SHARED_ENTITY_SHARE_LIMIT, ClassType.PAYABLE_CONTRACT, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, INT);
    private static final ConstructorSignature OFFER_CONSTRUCTOR = new ConstructorSignature(OFFER, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, LONG);
    private static final BigInteger _200_000 = BigInteger.valueOf(200_000);
    private StorageReference creator;
    private StorageReference seller;
    private StorageReference buyer;
    private TransactionReference classpath_takamaka_code;

    @BeforeEach
    void beforeEach() throws Exception {
        setNode(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        creator = account(0);
        seller = account(1);
        buyer = account(2);
        classpath_takamaka_code = takamakaCode();
    }


    @Test
    @DisplayName("SharedEntity place offer")
    void place() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {

        // Creation of the shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TWO));

        // Create 2 offers by the seller
        StorageReference offer1 = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, new BigIntegerValue(BigInteger.TEN), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));
        StorageReference offer2 = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // Invalid: seller tries to sell more shares than he own
        throwsTransactionExceptionWithCause("io.takamaka.code.lang.RequirementViolationException", () ->
                addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY, "place", ClassType.BIG_INTEGER, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer1)
        );

        // Invalid: the creator places the seller offer
        throwsTransactionExceptionWithCause("io.takamaka.code.lang.RequirementViolationException", () ->
                addInstanceMethodCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY, "place", ClassType.BIG_INTEGER, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer2)
        );

        // The seller places his second offer
        // Valid
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY, "place", ClassType.BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer2);
    }

    @Test
    @DisplayName("SharedEntity accept offer")
    void accept() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {

        // Creation of the shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN));

        // Creation of the offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code, OFFER_CONSTRUCTOR,
                new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // The seller places his offer
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY, "place", ClassType.BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

        // Invalid: buyer uses insufficient money
        throwsTransactionExceptionWithCause("io.takamaka.code.lang.RequirementViolationException", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY, "accept", ClassType.BIG_INTEGER, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer)
        );

        // Valid: buyer accepts the offer
        addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY, "accept", ClassType.BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.TWO), offer);
    }

    @Test
    @DisplayName("SharedEntityMaxShareholders limit reached (initialization)")
    void maxShareholdersLimitReachedInitialization() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {

        // Invalid: limit reached in initialization
        throwsTransactionExceptionWithCause("io.takamaka.code.lang.RequirementViolationException", () ->
                addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                        SHARED_ENTITY_MAX_SHAREHOLDERS_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(0))
        );

        // Valid
        addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_MAX_SHAREHOLDERS_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(1));
    }

    @Test
    @DisplayName("SharedEntityMaxShareholders limit reached")
    void maxShareholdersLimitReached() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {

        // Create 2 sharedEntities
        StorageReference sharedEntity1 = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_MAX_SHAREHOLDERS_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(1));
        StorageReference sharedEntity2 = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_MAX_SHAREHOLDERS_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(2));

        // Creation of an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // Seller places the offer on both sharedEntities
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_MAX_SHAREHOLDERS, "place", ClassType.BIG_INTEGER, OFFER),
                sharedEntity1, new BigIntegerValue(BigInteger.ZERO), offer);
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_MAX_SHAREHOLDERS, "place", ClassType.BIG_INTEGER, OFFER),
                sharedEntity2, new BigIntegerValue(BigInteger.ZERO), offer);

        // Buyer accepts both offers
        // Invalid: max shareholder limit reached
        throwsTransactionExceptionWithCause("io.takamaka.code.lang.RequirementViolationException", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY_MAX_SHAREHOLDERS, "accept", ClassType.BIG_INTEGER, OFFER),
                        sharedEntity1, new BigIntegerValue(BigInteger.TEN), offer)
        );

        // Valid
        addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_MAX_SHAREHOLDERS, "accept", ClassType.BIG_INTEGER, OFFER),
                sharedEntity2, new BigIntegerValue(BigInteger.TEN), offer);
    }

    @Test
    @DisplayName("SharedEntitySellAllShares place offers")
    void sellAllSharesError() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {

        // Create a SharedEntityMaxShareholders
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_SELL_ALL_SHARES_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN));

        // Creates 2 offers
        StorageReference offer1 = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        StorageReference offer2 = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, new BigIntegerValue(BigInteger.TEN), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // Places 2 offers
        // Invalid: seller tries to sell 2 of 10 shares
        throwsTransactionExceptionWithCause("io.takamaka.code.lang.RequirementViolationException", () ->
                addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY_SELL_ALL_SHARES, "place", ClassType.BIG_INTEGER, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer1)
        );

        // Valid: seller tries to sell 10 to 10 shares
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_SELL_ALL_SHARES, "place", ClassType.BIG_INTEGER, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer2);

    }

    @Test
    @DisplayName("SharedEntityShareLimit limit reached in initialization")
    void shareLimitReachedInitialization() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {

        // Valid
        addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_SHARE_LIMIT_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(100));

        // Invalid: shareLimit reached in initialization
        throwsTransactionExceptionWithCause("io.takamaka.code.lang.RequirementViolationException", () ->
                addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                        SHARED_ENTITY_SHARE_LIMIT_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(50))
        );

        // Invalid: shareLimit value not valid
        throwsTransactionExceptionWithCause("io.takamaka.code.lang.RequirementViolationException", () ->
                addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                        SHARED_ENTITY_SHARE_LIMIT_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(0))
        );
        throwsTransactionExceptionWithCause("io.takamaka.code.lang.RequirementViolationException", () ->
                addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                        SHARED_ENTITY_SHARE_LIMIT_CONSTRUCTOR, seller, new BigIntegerValue(BigInteger.TEN), new IntValue(101))
        );
    }

    @Test
    @DisplayName("SharedEntityShareLimit limit reached when accept offer")
    void shareLimitReached() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {

        // Create 2 sharedEntity with 2 shareholders
        StorageReference sharedEntity1 = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_SHARE_LIMIT_CONSTRUCTOR_2, seller, buyer, new BigIntegerValue(BigInteger.TEN), new BigIntegerValue(BigInteger.TEN), new IntValue(50));
        StorageReference sharedEntity2 = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_SHARE_LIMIT_CONSTRUCTOR_2, seller, buyer, new BigIntegerValue(BigInteger.TEN), new BigIntegerValue(BigInteger.TEN), new IntValue(70));

        // Create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // Places the offer on both cases
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_SHARE_LIMIT, "place", ClassType.BIG_INTEGER, OFFER),
                sharedEntity1, new BigIntegerValue(BigInteger.ZERO), offer);
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_SHARE_LIMIT, "place", ClassType.BIG_INTEGER, OFFER),
                sharedEntity2, new BigIntegerValue(BigInteger.ZERO), offer);

        // Buyer accepts on both cases
        // Invalid: share limit reached
        throwsTransactionExceptionWithCause("io.takamaka.code.lang.RequirementViolationException", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(SHARED_ENTITY_SHARE_LIMIT, "accept", ClassType.BIG_INTEGER, OFFER),
                        sharedEntity1, new BigIntegerValue(BigInteger.TWO), offer)
        );

        // Valid
        addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(SHARED_ENTITY_SHARE_LIMIT, "accept", ClassType.BIG_INTEGER, OFFER),
                sharedEntity2, new BigIntegerValue(BigInteger.TWO), offer);
    }
}

