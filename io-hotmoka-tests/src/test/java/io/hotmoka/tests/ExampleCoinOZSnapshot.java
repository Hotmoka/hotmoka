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

import static io.hotmoka.helpers.Coin.filicudi;
import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.helpers.Coin.stromboli;
import static io.hotmoka.node.StorageTypes.BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.StorageReference;
import io.takamaka.code.constants.Constants;

/**
 * A test for the ExampleCoinOZSnapshot contract (a ERC20OZSnapshot contract).
 */
class ExampleCoinOZSnapshot extends HotmokaTest {
    private static final ClassType EXCOZS = StorageTypes.classNamed("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot");
    private static final ClassType UBI = StorageTypes.UNSIGNED_BIG_INTEGER;
    private static final ConstructorSignature CONSTRUCTOR_EXCOZS = ConstructorSignatures.of(EXCOZS);
    private static final ConstructorSignature CONSTRUCTOR_UBI_STR = ConstructorSignatures.of(UBI, StorageTypes.STRING);

    /**
     * The classpath of the classes of code module.
     */
    private TransactionReference classpath_takamaka_code;

    /**
     * The creator of the coin.
     */
    private StorageReference creator;
    private PrivateKey creator_prv_key;

    /**
     * An investor.
     */
    private StorageReference investor1;
    private PrivateKey investor1_prv_key;

    /**
     * Another investor.
     */
    private StorageReference investor2;

    @BeforeAll
	static void beforeAll() throws Exception {
		setJar("tokens.jar");
	}
    
    @BeforeEach
    void beforeEach() throws Exception {
        setAccounts(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        creator = account(1);
        investor1 = account(2);
        investor2 = account(3);
        creator_prv_key = privateKey(1);
        investor1_prv_key = privateKey(2);
        classpath_takamaka_code = takamakaCode();
    }

    @Test @DisplayName("new ExampleCoinOZSnapshot()")
    void createExampleCoinOZSnapshot() throws Exception {
        addConstructorCallTransaction(
                creator_prv_key, // an object that signs with the payer's private key
                creator, // payer of the transaction
                _500_000, // gas provided to the transaction
                panarea(1), // gas price
                jar(), //reference to the jar being tested
                CONSTRUCTOR_EXCOZS // constructor signature
        );
    }

    @Test @DisplayName("Test of ERC20OZSnapshot _snapshot method: example_token.yieldSnapshot() == 1")
    void yieldSnapshot() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCOZS);

        StorageReference current_snapshot_id = (StorageReference) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // current_snapshot_id = example_token.yieldSnapshot() == 1

        BigIntegerValue current_snapshot_id_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(
                creator,
                _500_000, classpath_takamaka_code,
                MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER),
                current_snapshot_id);
        // 1.toBigInteger() == BigInteger@1

        /* # SNAPSHOTS #
        creator = {}
        totalSupply = {}
        */

        assertEquals(current_snapshot_id_ub.getValue(), BigInteger.ONE);
    }

    /**
     ***** CHRONOLOGY
     * creator@mint(200000000000000000000000, creator) > [creator:200000000000000000000000]
     ***** FINAL STATE OF SNAPSHOTS
     * {}
     ***** QUESTIONS
     * Until the first snapshot is taken, all the balances can be obtained directly with balanceOf() and totalSupply()
     * According to the ERC20OZSnapshot specification, snapshot 0 does not exist.
     */
    @Test @DisplayName("Full test of ERC20OZSnapshot #1, Exception: snapshot 0 does not exist")
    void fullTest1_Exception() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCOZS);
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1"));

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceNonVoidMethodCallTransaction(
                        creator_prv_key, creator,
                        _500_000, panarea(1), jar(),
                        MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI),
                        example_token,
                        creator, ubi_0)
        );
    }

    /**
     ***** CHRONOLOGY
     * creator@mint(200000000000000000000000, creator) > [creator:200000000000000000000000]
     * creator@yieldSnapshot() >> 1
     * creator@transfer(5000, investor1) > [creator:199999999999999999995000, investor1:5000]
     ***** FINAL STATE OF SNAPSHOTS
     * creator = {1:200000000000000000000000} investor1={1:0}
     ***** QUESTIONS
     * · What is the balance of creator at time 1? 200000000000000000000000 because 0 != 1
     * · What is the balance of investor1 at time 1? 0 because 0 != 1
     * . What is the balance of investor2 at time 1? :0 because 0 == 0
     * @throws InterruptedException 
     * @throws TimeoutException 
     * @throws NodeException 
     */
    @Test @DisplayName("Full test of ERC20OZSnapshot #2")
    void fullTest2() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCOZS);
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("5000"));

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 1

        var transfer_result = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_5000);
        //vcreator@transfer(5000, investor1) > [creator:199999999999999999995000, investor1:5000]
        assertTrue(transfer_result.getValue());

        StorageReference ubi_1 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1"));

        StorageReference creator_balance_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_1);
        BigIntegerValue creator_balance_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time1);
        assertEquals(creator_balance_time1_ub.getValue(), new BigInteger("200000000000000000000000"));

        StorageReference creator_investor1_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_1);
        BigIntegerValue creator_investor1_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time1);
        assertEquals(creator_investor1_time1_ub.getValue(), BigInteger.ZERO);

        StorageReference creator_investor2_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_1);
        BigIntegerValue creator_investor2_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time1);
        assertEquals(creator_investor2_time1_ub.getValue(), BigInteger.ZERO);
    }

    /**
     ***** CHRONOLOGY
     * creator@mint(200000000000000000000000, creator) > [creator:200000000000000000000000]
     * creator@yieldSnapshot() >> 1
     * creator@transfer(5000, investor1) > [creator:199999999999999999995000, investor1:5000]
     * creator@transfer(4000, investor2) > [creator:199999999999999999991000, investor1:5000, investor2:4000]
     ***** FINAL STATE OF SNAPSHOTS
     * creator = {1:200000000000000000000000} investor1={1:0} investor2={1:0}
     ***** QUESTIONS
     * · What is the balance of creator at time 1? 200000000000000000000000 because 0 != 1
     * · What is the balance of investor1 at time 1? 0 because 0 != 1
     * · What is the balance of investor2 at time 1? 0 because 0 != 1
     * @throws InterruptedException 
     * @throws TimeoutException 
     * @throws NodeException 
     */
    @Test @DisplayName("Full test of ERC20OZSnapshot #3")
    void fullTest3() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCOZS);
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("5000"));
        StorageReference ubi_4000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("4000"));

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 1

        BooleanValue transfer_result = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_5000);
        // creator@transfer(5000, investor1) > [creator:199999999999999999995000, investor1:5000]
        assertTrue(transfer_result.getValue());

        BooleanValue transfer_result2 = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor2, ubi_4000);
        // creator@transfer(4000, investor2) > [creator:199999999999999999991000, investor1:5000, investor2:4000]
        assertTrue(transfer_result2.getValue());

        StorageReference ubi_1 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1"));

        StorageReference creator_balance_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_1);
        BigIntegerValue creator_balance_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time1);
        assertEquals(creator_balance_time1_ub.getValue(), new BigInteger("200000000000000000000000"));

        StorageReference creator_investor1_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_1);
        BigIntegerValue creator_investor1_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time1);
        assertEquals(creator_investor1_time1_ub.getValue(), BigInteger.ZERO);

        StorageReference creator_investor2_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_1);
        BigIntegerValue creator_investor2_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time1);
        assertEquals(creator_investor2_time1_ub.getValue(), BigInteger.ZERO);
    }

    /**
     ***** CHRONOLOGY
     * creator@mint(200000000000000000000000, creator) > [creator:200000000000000000000000]
     * creator@yieldSnapshot() >> 1
     * creator@transfer(5000, investor1) > [creator:199999999999999999995000, investor1:5000]
     * creator@yieldSnapshot() >> 2
     * creator@transfer(3000, investor1) > [creator:199999999999999999992000, investor1:8000]
     ***** FINAL STATE OF SNAPSHOTS
     * creator = {1:200000000000000000000000, 2:199999999999999999995000} investor1={1:0, 2:5000}
     ***** QUESTIONS
     * · What is the balance of creator at time 1? 200000000000000000000000 because 0 != 2
     * · What is the balance of investor1 at time 1? 0 because 0 != 2
     * · What is the balance of investor2 at time 1? 0 because 0 == 0
     * · What is the balance of creator at time 2? 199999999999999999995000 because 1 != 2
     * · What is the balance of investor1 at time 2? 5000 because 1 != 2
     * · What is the balance of investor2 at time 2? 0 because 0 == 0
     * @throws InterruptedException 
     * @throws TimeoutException 
     * @throws NodeException 
     */
    @Test @DisplayName("Full test of ERC20OZSnapshot #4")
    void fullTest4() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCOZS);
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("5000"));
        StorageReference ubi_3000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("3000"));

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 1

        BooleanValue transfer_result = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_5000);
        // creator@transfer(5000, investor1) > [creator:199999999999999999995000, investor1:5000]
        assertTrue(transfer_result.getValue());

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 2

        BooleanValue transfer_result2 = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_3000);
        // creator@transfer(3000, investor1) > [creator:199999999999999999992000, investor1:8000]
        assertTrue(transfer_result2.getValue());

        StorageReference ubi_1 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1"));
        StorageReference ubi_2 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("2"));

        StorageReference creator_balance_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_1);
        BigIntegerValue creator_balance_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time1);
        assertEquals(creator_balance_time1_ub.getValue(), new BigInteger("200000000000000000000000"));

        StorageReference creator_investor1_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_1);
        BigIntegerValue creator_investor1_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time1);
        assertEquals(creator_investor1_time1_ub.getValue(), BigInteger.ZERO);

        StorageReference creator_investor2_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_1);
        BigIntegerValue creator_investor2_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time1);
        assertEquals(creator_investor2_time1_ub.getValue(), BigInteger.ZERO);

        StorageReference creator_balance_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_2);
        BigIntegerValue creator_balance_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time2);
        assertEquals(creator_balance_time2_ub.getValue(), new BigInteger("199999999999999999995000"));

        StorageReference creator_investor1_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_2);
        BigIntegerValue creator_investor1_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time2);
        assertEquals(creator_investor1_time2_ub.getValue(), new BigInteger("5000"));

        StorageReference creator_investor2_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_2);
        BigIntegerValue creator_investor2_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time2);
        assertEquals(creator_investor2_time2_ub.getValue(), BigInteger.ZERO);
    }

    /**
     ***** CHRONOLOGY
     * creator@mint(200000000000000000000000, creator) > [creator:200000000000000000000000]
     * creator@yieldSnapshot() >> 1
     * creator@transfer(5000, investor1) > [creator:199999999999999999995000, investor1:5000]
     * creator@yieldSnapshot() >> 2
     * creator@yieldSnapshot() >> 3
     * creator@transfer(3000, investor1) > [creator:199999999999999999992000, investor1:8000]
     ***** FINAL STATE OF SNAPSHOTS
     * creator = {1:200000000000000000000000, 3:199999999999999999995000} investor1={1:0, 3:5000} totalSupply = {}
     ***** QUESTIONS
     * · What is the balance of creator at time 1? 200000000000000000000000 because 0 != 2
     * · What is the balance of investor1 at time 1? 0 because 0 != 2
     * · What is the balance of investor2 at time 1? 0 because 0 == 0
     * · What is the totalSupply at time 1? :200000000000000000000000 because 0 == 0
     * · What is the balance of creator at time 2? 199999999999999999995000 because 1 != 2
     * · What is the balance of investor1 at time 2? 0 because 1 != 2
     * · What is the balance of investor2 at time 2? 0 because 0 == 0
     * · What is the totalSupply at time 2? :200000000000000000000000 because 0 == 0
     * · What is the balance of creator at time 3? 199999999999999999995000 because 1 != 2
     * · What is the balance of investor1 at time 3? 0 because 1 != 2
     * · What is the balance of investor2 at time 3? 0 because 0 == 0
     * · What is the totalSupply at time 3? :200000000000000000000000 because 0 == 0
     * @throws InterruptedException 
     * @throws TimeoutException 
     * @throws NodeException 
     */
    @Test @DisplayName("Full test of ERC20OZSnapshot #5")
    void fullTest5() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCOZS);
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("5000"));
        StorageReference ubi_3000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("3000"));

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 1

        BooleanValue transfer_result = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_5000);
        // creator@transfer(5000, investor1) > [creator:199999999999999999995000, investor1:5000]
        assertTrue(transfer_result.getValue());

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 2

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 3

        BooleanValue transfer_result2 = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_3000);
        // creator@transfer(3000, investor1) > [creator:199999999999999999992000, investor1:8000]
        assertTrue(transfer_result2.getValue());

        StorageReference ubi_1 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1"));
        StorageReference ubi_2 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("2"));
        StorageReference ubi_3 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("3"));

        StorageReference creator_balance_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_1);
        BigIntegerValue creator_balance_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time1);
        assertEquals(creator_balance_time1_ub.getValue(), new BigInteger("200000000000000000000000"));

        StorageReference creator_investor1_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_1);
        BigIntegerValue creator_investor1_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time1);
        assertEquals(creator_investor1_time1_ub.getValue(), BigInteger.ZERO);

        StorageReference creator_investor2_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_1);
        BigIntegerValue creator_investor2_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time1);
        assertEquals(creator_investor2_time1_ub.getValue(), BigInteger.ZERO);

        StorageReference totalSupply_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "totalSupplyAt", UBI, UBI), example_token, ubi_1);
        BigIntegerValue totalSupply_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), totalSupply_time1);
        assertEquals(totalSupply_time1_ub.getValue(), new BigInteger("200000000000000000000000"));

        StorageReference creator_balance_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_2);
        BigIntegerValue creator_balance_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time2);
        assertEquals(creator_balance_time2_ub.getValue(), new BigInteger("199999999999999999995000"));

        StorageReference creator_investor1_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_2);
        BigIntegerValue creator_investor1_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time2);
        assertEquals(creator_investor1_time2_ub.getValue(), new BigInteger("5000"));

        StorageReference creator_investor2_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_2);
        BigIntegerValue creator_investor2_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time2);
        assertEquals(creator_investor2_time2_ub.getValue(), BigInteger.ZERO);

        StorageReference totalSupply_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "totalSupplyAt", UBI, UBI), example_token, ubi_2);
        BigIntegerValue totalSupply_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), totalSupply_time2);
        assertEquals(totalSupply_time2_ub.getValue(), new BigInteger("200000000000000000000000"));

        StorageReference creator_balance_time3 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_3);
        BigIntegerValue creator_balance_time3_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time3);
        assertEquals(creator_balance_time3_ub.getValue(), new BigInteger("199999999999999999995000"));

        StorageReference creator_investor1_time3 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_3);
        BigIntegerValue creator_investor1_time3_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time3);
        assertEquals(creator_investor1_time3_ub.getValue(), new BigInteger("5000"));

        StorageReference creator_investor2_time3 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_3);
        BigIntegerValue creator_investor2_time3_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time3);
        assertEquals(creator_investor2_time3_ub.getValue(), BigInteger.ZERO);

        StorageReference totalSupply_time3 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "totalSupplyAt", UBI, UBI), example_token, ubi_3);
        BigIntegerValue totalSupply_time3_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), totalSupply_time3);
        assertEquals(totalSupply_time3_ub.getValue(), new BigInteger("200000000000000000000000"));
    }

    /**
     ***** CHRONOLOGY
     * creator@mint(200000000000000000000000, creator) > [creator:200000000000000000000000]
     * creator@yieldSnapshot() >> 1
     * creator@transfer(5000, investor1) > [creator:199999999999999999995000, investor1:5000]
     * creator@transfer(5000, investor1) > [creator:199999999999999999990000, investor1:10000]
     * creator@burn(5000, creator) > [creator:199999999999999999985000, investor1:10000]  TotalSupply= 199999999999999999995000
     * creator@yieldSnapshot() >> 2
     * creator@yieldSnapshot() >> 3
     * investor1@transfer(1000, investor2) > [creator:199999999999999999985000, investor1:9000, investor2:1000] TotalSupply= 199999999999999999995000
     * creator@yieldSnapshot() >> 4
     * creator@burn(5000, creator) > [creator:199999999999999999980000, investor1:9000, investor2:1000] TotalSupply= 199999999999999999990000
     ***** FINAL STATE OF SNAPSHOTS
     * creator = {1:200000000000000000000000, 4:199999999999999999985000}
     * investor1={1:0, 3:10000}
     * investor2={3:0}
     * TotalSupply={1:200000000000000000000000, 4:199999999999999999995000}
     ***** QUESTIONS
     * · What is the balance of creator at time 1? 200000000000000000000000 because 0 != 2
     * · What is the balance of investor1 at time 1? 0 because 0 != 2
     * · What is the balance of investor2 at time 1? 0 because 0 != 1
     * · What is the totalSupply at time 1? 200000000000000000000000 because 0 != 2
     * · What is the balance of creator at time 2? 199999999999999999985000 because 1 != 2
     * · What is the balance of investor1 at time 2? 10000 because 1 != 2
     * · What is the balance of investor2 at time 2? 0 because 0 != 1
     * · What is the totalSupply at time 2? 199999999999999999995000 because 1 != 2
     * · What is the balance of creator at time 3? 199999999999999999985000 because 1 != 2
     * · What is the balance of investor1 at time 3? 10000 because 1 != 2
     * · What is the balance of investor2 at time 3? 0 because 0 != 1
     * · What is the totalSupply at time 3? 199999999999999999995000 because 1 != 2
     * · What is the balance of creator at time 4? 199999999999999999985000 because 1 != 2
     * · What is the balance of investor1 at time 4? :9000 because 2 == 2
     * · What is the balance of investor2 at time 4? :1000 because 1 == 1
     * · What is the totalSupply at time 4? :199999999999999999985000 because 1 != 2
     * @throws InterruptedException 
     * @throws TimeoutException 
     * @throws NodeException 
     */
    @Test @DisplayName("Full test of ERC20OZSnapshot #6")
    void fullTest6() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCOZS);
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("5000"));
        StorageReference ubi_1000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1000"));

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 1

        BooleanValue transfer_result = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_5000);
        // creator@transfer(5000, investor1) > [creator:199999999999999999995000, investor1:5000]
        assertTrue(transfer_result.getValue());

        BooleanValue transfer_result2 = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_5000);
        // creator@transfer(5000, investor1) > [creator:199999999999999999990000, investor1:10000]
        assertTrue(transfer_result2.getValue());

        addInstanceVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofVoid(EXCOZS, "burn", StorageTypes.CONTRACT, UBI),
                example_token,
                creator, ubi_5000);
        // creator@burn(5000, creator) > [creator:199999999999999999985000, investor1:10000]  TotalSupply= 199999999999999999995000

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 2

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 3

        addInstanceNonVoidMethodCallTransaction(
                investor1_prv_key, investor1,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor2, ubi_1000);
        // investor1@transfer(1000, investor2) > [creator:199999999999999999985000, investor1:9000, investor2:1000] TotalSupply= 199999999999999999995000

        addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCOZS, "yieldSnapshot", UBI),
                example_token);
        // creator@yieldSnapshot() >> 4

        addInstanceVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofVoid(EXCOZS, "burn", StorageTypes.CONTRACT, UBI),
                example_token,
                creator, ubi_5000);
        // creator@burn(5000, creator) > [creator:199999999999999999980000, investor1:9000, investor2:1000] TotalSupply= 199999999999999999990000

        StorageReference ubi_1 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1"));
        StorageReference ubi_2 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("2"));
        StorageReference ubi_3 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("3"));
        StorageReference ubi_4 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("4"));

        StorageReference creator_balance_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_1);
        BigIntegerValue creator_balance_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time1);
        assertEquals(creator_balance_time1_ub.getValue(), new BigInteger("200000000000000000000000"));

        StorageReference creator_investor1_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_1);
        BigIntegerValue creator_investor1_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time1);
        assertEquals(creator_investor1_time1_ub.getValue(), BigInteger.ZERO);

        StorageReference creator_investor2_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_1);
        BigIntegerValue creator_investor2_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time1);
        assertEquals(creator_investor2_time1_ub.getValue(), BigInteger.ZERO);

        StorageReference totalSupply_time1 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "totalSupplyAt", UBI, UBI), example_token, ubi_1);
        BigIntegerValue totalSupply_time1_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), totalSupply_time1);
        assertEquals(totalSupply_time1_ub.getValue(), new BigInteger("200000000000000000000000"));

        StorageReference creator_balance_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_2);
        BigIntegerValue creator_balance_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time2);
        assertEquals(creator_balance_time2_ub.getValue(), new BigInteger("199999999999999999985000"));

        StorageReference creator_investor1_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_2);
        BigIntegerValue creator_investor1_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time2);
        assertEquals(creator_investor1_time2_ub.getValue(), new BigInteger("10000"));

        StorageReference creator_investor2_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_2);
        BigIntegerValue creator_investor2_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time2);
        assertEquals(creator_investor2_time2_ub.getValue(), BigInteger.ZERO);

        StorageReference totalSupply_time2 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "totalSupplyAt", UBI, UBI), example_token, ubi_2);
        BigIntegerValue totalSupply_time2_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), totalSupply_time2);
        assertEquals(totalSupply_time2_ub.getValue(), new BigInteger("199999999999999999995000"));

        StorageReference creator_balance_time3 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_3);
        BigIntegerValue creator_balance_time3_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time3);
        assertEquals(creator_balance_time3_ub.getValue(), new BigInteger("199999999999999999985000"));

        StorageReference creator_investor1_time3 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_3);
        BigIntegerValue creator_investor1_time3_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time3);
        assertEquals(creator_investor1_time3_ub.getValue(), new BigInteger("10000"));

        StorageReference creator_investor2_time3 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_3);
        BigIntegerValue creator_investor2_time3_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time3);
        assertEquals(creator_investor2_time3_ub.getValue(), BigInteger.ZERO);

        StorageReference totalSupply_time3 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "totalSupplyAt", UBI, UBI), example_token, ubi_3);
        BigIntegerValue totalSupply_time3_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), totalSupply_time3);
        assertEquals(totalSupply_time3_ub.getValue(), new BigInteger("199999999999999999995000"));

        StorageReference creator_balance_time4 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, creator, ubi_4);
        BigIntegerValue creator_balance_time4_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_balance_time4);
        assertEquals(creator_balance_time4_ub.getValue(), new BigInteger("199999999999999999985000"));

        StorageReference creator_investor1_time4 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor1, ubi_4);
        BigIntegerValue creator_investor1_time4_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor1_time4);
        assertEquals(creator_investor1_time4_ub.getValue(), new BigInteger("9000"));

        StorageReference creator_investor2_time4 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "balanceOfAt", UBI, StorageTypes.CONTRACT, UBI), example_token, investor2, ubi_4);
        BigIntegerValue creator_investor2_time4_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), creator_investor2_time4);
        assertEquals(creator_investor2_time4_ub.getValue(), new BigInteger("1000"));

        StorageReference totalSupply_time4 = (StorageReference) addInstanceNonVoidMethodCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), MethodSignatures.ofNonVoid(EXCOZS, "totalSupplyAt", UBI, UBI), example_token, ubi_4);
        BigIntegerValue totalSupply_time4_ub = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(creator, _500_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER), totalSupply_time4);
        assertEquals(totalSupply_time4_ub.getValue(), new BigInteger("199999999999999999995000"));
    }
}