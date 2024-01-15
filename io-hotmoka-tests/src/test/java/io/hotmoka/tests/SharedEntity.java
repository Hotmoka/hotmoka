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

import static io.hotmoka.beans.Coin.filicudi;
import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.Coin.stromboli;
import static io.hotmoka.beans.StorageTypes.BIG_INTEGER;
import static io.hotmoka.beans.StorageTypes.INT;
import static io.hotmoka.beans.StorageTypes.LONG;
import static io.hotmoka.beans.StorageTypes.PAYABLE_CONTRACT;
import static io.hotmoka.beans.StorageTypes.SHARED_ENTITY;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

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
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.StorageReference;

/**
 * A test for the shared entity contract and subclasses.
 */
class SharedEntity extends HotmokaTest {
    private static final ClassType SIMPLE_SHARED_ENTITY = StorageTypes.classNamed("io.takamaka.code.dao.SimpleSharedEntity");
    private static final ClassType SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS = StorageTypes.classNamed(SHARED_ENTITY + "WithCappedShareholders");
    private static final ClassType SHARED_ENTITY_WITH_INTEGRAL_SHARES = StorageTypes.classNamed(SHARED_ENTITY + "WithIntegralShares");
    private static final ClassType SHARED_ENTITY_WITH_CAPPED_SHARES = StorageTypes.classNamed(SHARED_ENTITY + "WithCappedShares");
    private static final ClassType OFFER = StorageTypes.SHARED_ENTITY_OFFER;
    private static final ConstructorSignature SIMPLE_SHARED_ENTITY_CONSTRUCTOR = ConstructorSignatures.of(SIMPLE_SHARED_ENTITY, PAYABLE_CONTRACT, BIG_INTEGER);
    private static final ConstructorSignature SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS_CONSTRUCTOR = ConstructorSignatures.of(SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS, PAYABLE_CONTRACT, BIG_INTEGER, INT);
    private static final ConstructorSignature SHARED_ENTITY_WITH_INTEGRAL_SHARES_CONSTRUCTOR = ConstructorSignatures.of(SHARED_ENTITY_WITH_INTEGRAL_SHARES, PAYABLE_CONTRACT, BIG_INTEGER);
    private static final ConstructorSignature SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR = ConstructorSignatures.of(SHARED_ENTITY_WITH_CAPPED_SHARES, PAYABLE_CONTRACT, BIG_INTEGER, INT);
    private static final ConstructorSignature SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR_2 = ConstructorSignatures.of(SHARED_ENTITY_WITH_CAPPED_SHARES, PAYABLE_CONTRACT, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, INT);
    private static final ConstructorSignature OFFER_CONSTRUCTOR = ConstructorSignatures.of(OFFER, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, LONG);
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
        		SIMPLE_SHARED_ENTITY_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(2));

        // create an offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // invalid: the seller is trying to sell more shares than it owns
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "the seller has not enough shares to sell", () ->
                addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                		MethodSignatures.ofVoid(SHARED_ENTITY, "place", BIG_INTEGER, OFFER),
                        sharedEntity, StorageValues.bigIntegerOf(0), offer)
        );
    }

    @Test
    @DisplayName("a contract cannot place an offer on behalf of somebody else")
    void placeOnBehalfOfAnotherIsRejected() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SIMPLE_SHARED_ENTITY_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(2));

        // create an offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // invalid: the creator is trying to place the offer on behalf of the seller
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "only the seller can place its own offer", () ->
                addInstanceMethodCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                		MethodSignatures.ofVoid(SHARED_ENTITY, "place", BIG_INTEGER, OFFER),
                        sharedEntity, StorageValues.bigIntegerOf(0), offer)
        );
    }

    @Test
    @DisplayName("a contract can place its own offer")
    void placeOnBehalfOfOneselfWorks() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SIMPLE_SHARED_ENTITY_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(2));

        // create an offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // valid: the seller places the second offer
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY, "place", BIG_INTEGER, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(0), offer);
    }

    @Test
    @DisplayName("acceptance with too little money is rejected")
    void acceptanceWithTooLittleMoneyIsRejected() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SIMPLE_SHARED_ENTITY_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10));

        // create an offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code, OFFER_CONSTRUCTOR,
                seller, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // the seller places his offer
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY, "place", BIG_INTEGER, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(0), offer);

        // invalid: the buyer provides too little money
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "not enough money to accept the offer", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
                		MethodSignatures.ofVoid(SHARED_ENTITY, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                        sharedEntity, StorageValues.bigIntegerOf(0), buyer, offer)
        );
    }

    @Test
    @DisplayName("acceptance with enough money works")
    void acceptanceWithEnoughMoneyWorks() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity contract
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SIMPLE_SHARED_ENTITY_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10));

        // create an offer by the seller
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code, OFFER_CONSTRUCTOR,
                seller, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // the seller places his offer
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY, "place", BIG_INTEGER, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(0), offer);

        // valid: the buyer provides enough money to accept the offer
        addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(2), buyer, offer);
    }

    @Test
    @DisplayName("more shareholders than the capped shareholders limit are rejected at initialization")
    void maxShareholdersLimitViolatedAtInitialization() {
        // invalid: limit reached in initialization
    	throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "too many shareholders", () ->
                addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                        SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.intOf(0))
        );
    }

    @Test
    @DisplayName("fewer shareholders than the capped shareholders limit are accepted at initialization")
    void maxShareholdersLimitRespectedAtInitialization() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // valid
        addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.intOf(1));
    }

    @Test
    @DisplayName("capped shareholders limit works when accepting an offer that crosses the cap")
    void maxShareholdersLimitViolatedAtAcceptance() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.intOf(1));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // the seller places the offer
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS, "place", BIG_INTEGER, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(0), offer);

        // invalid: the maximal limit of shareholders has been reached
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "too many shareholders, the limit is 1", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
                		MethodSignatures.ofVoid(SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                        sharedEntity, StorageValues.bigIntegerOf(10), buyer, offer)
        );
    }

    @Test
    @DisplayName("capped shareholders limit works when accepting an offer that does not cross the cap")
    void maxShareholdersLimitRespectedAtAcceptance() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.intOf(2));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS, "place", BIG_INTEGER, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(0), offer);

        // valid
        addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY_WITH_CAPPED_SHAREHOLDERS, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(10), buyer, offer);
    }

    @Test
    @DisplayName("an attempt to sell only a part of the shares fails")
    void attemptToSellOnlyPartOfSharesFails() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_INTEGRAL_SHARES_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // invalid: the seller tries to sell only 2 of its 10 shares
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "the seller must sell its shares in full", () ->
                addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                		MethodSignatures.ofVoid(SHARED_ENTITY_WITH_INTEGRAL_SHARES, "place", BIG_INTEGER, OFFER),
                        sharedEntity, StorageValues.bigIntegerOf(0), offer)
        );
    }

    @Test
    @DisplayName("an attempt to sell all own shares succeeds")
    void attemptToSellAllSharesSucceeds() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_INTEGRAL_SHARES_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // valid: the seller sells all its 10 shares
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY_WITH_INTEGRAL_SHARES, "place", BIG_INTEGER, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(0), offer);

    }

    @Test
    @DisplayName("the maximal percent of shares is respected at initialization")
    void shareLimitReachedAtInitialization() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.intOf(100));

        // invalid: the maximal limit of shares is reached at initialization
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "a shareholder cannot hold more than", () ->
                addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.intOf(50))
        );
    }

    @Test
    @DisplayName("the maximal percent of shares cannot be smaller than one")
    void shareLimitIsSmallerThan1() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.intOf(100));

        // invalid: the limit percent of shares is not positive
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "invalid share limit", () ->
                addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.intOf(0))
        );
    }

    @Test
    @DisplayName("the maximal percent of shares cannot be larger than 100")
    void shareLimitIsLargerThan100() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
        		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.intOf(100));

        // invalid: the limit percent of shares is larger than 100
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "invalid share limit", () ->
                addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                		SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(10), StorageValues.intOf(101))
        );
    }

    @Test
    @DisplayName("the maximal percent of shares is respected when accepting an offer that stays inside the limit")
    void sharePercentLimitNotReached() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
    	// create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR_2, seller, buyer, StorageValues.bigIntegerOf(10), StorageValues.bigIntegerOf(10), StorageValues.intOf(70));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // place the offer on the shared entity
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY_WITH_CAPPED_SHARES, "place", BIG_INTEGER, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(0), offer);

        // valid
        addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY_WITH_CAPPED_SHARES, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(2), buyer, offer);
    }

    @Test
    @DisplayName("the maximal percent of shares is respected when accepting an offer that goes beyond the limit")
    void sharePercentLimitReached() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create a shared entity
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _500_000, panarea(1), classpath_takamaka_code,
                SHARED_ENTITY_WITH_CAPPED_SHARES_CONSTRUCTOR_2, seller, buyer, StorageValues.bigIntegerOf(10), StorageValues.bigIntegerOf(10), StorageValues.intOf(50));

        // create an offer
        StorageReference offer = addConstructorCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
                OFFER_CONSTRUCTOR, seller, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // place the offer on the shared entity
        addInstanceMethodCallTransaction(privateKey(1), seller, _500_000, panarea(1), classpath_takamaka_code,
        		MethodSignatures.ofVoid(SHARED_ENTITY_WITH_CAPPED_SHARES, "place", BIG_INTEGER, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(0), offer);

        // invalid: the share limit has been reached
        throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "a shareholder cannot hold more than", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _500_000, panarea(1), classpath_takamaka_code,
                		MethodSignatures.ofVoid(SHARED_ENTITY_WITH_CAPPED_SHARES, "accept", BIG_INTEGER, PAYABLE_CONTRACT, OFFER),
                        sharedEntity, StorageValues.bigIntegerOf(2), buyer, offer)
        );
    }
}