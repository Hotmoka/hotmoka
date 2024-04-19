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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;

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
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.StorageReference;
import io.takamaka.code.constants.Constants;

/**
 * A test for the ExampleCoinBurnable contract (a ERC20Burnable contract).
 */
class ExampleCoinBurnable extends HotmokaTest {
    private static final ClassType EXCB = StorageTypes.classNamed("io.hotmoka.examples.tokens.ExampleCoinBurnable");
    private static final ClassType UBI = StorageTypes.UNSIGNED_BIG_INTEGER;
    private static final ConstructorSignature CONSTRUCTOR_EXCB = ConstructorSignatures.of(EXCB);
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
    private PrivateKey investor2_prv_key;

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
        investor2_prv_key = privateKey(3);
        classpath_takamaka_code = takamakaCode();
    }

    @Test @DisplayName("new ExampleCoinBurnable()")
    void createExampleCoinBurnable() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
        addConstructorCallTransaction(
                creator_prv_key, // an object that signs with the payer's private key
                creator, // payer of the transaction
                _500_000, // gas provided to the transaction
                panarea(1), // gas price
                jar(), //reference to the jar being tested
                CONSTRUCTOR_EXCB // constructor signature
                );
    }

    @Test
    @DisplayName("Test of ERC20Burnable burn method: example_token.burn(500'000) --> totalSupply-=500'000, balances[caller]-=500'000")
    void burn() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCB);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("199999999999999999500000"));
        StorageReference ubi_500000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("500000"));

        addInstanceVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                MethodSignatures.ofVoid(EXCB, "burn", UBI),
                example_token,
                ubi_500000);
        // balances = [creator:199999999999999999500000], totalSupply:199999999999999999500000

        StorageReference creator_balance = (StorageReference) runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.ofNonVoid(EXCB, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 199999999999999999500000
        BooleanValue equals_result1 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), creator_balance, ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18 - 500000) = true

        StorageReference supply = (StorageReference) runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, jar(),
                MethodSignatures.ofNonVoid(EXCB, "totalSupply", UBI),
                example_token);
        // supply = example_token.totalSupply() == 200'000*10^18 - 500000

        BooleanValue equals_result2 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), supply, ubi_check);
        // equals_result2 = supply.equals(200'000*10^18 - 500000) = true

        assertTrue(equals_result1.getValue() && equals_result2.getValue());
    }

    @Test
    @DisplayName("Test of ERC20Burnable burnFrom method: example_token.burnFrom(recipient, 500'000) --> totalSupply-=500'000, balances[recipient]-=500'000")
    void burnFrom() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCB);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("199999999999999999996000"));
        StorageReference ubi_7000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("7000"));
        StorageReference ubi_4000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("4000"));
        StorageReference ubi_3000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("3000"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("0"));

        BooleanValue approve_result = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCB, "approve", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_7000);
        // Now investor1 is able to spend or burn 7000 MiniEb for creator

        addInstanceVoidMethodCallTransaction(
                investor1_prv_key, investor1,
                _100_000, panarea(1), jar(),
                MethodSignatures.ofVoid(EXCB, "burnFrom", StorageTypes.CONTRACT, UBI),
                example_token,
                creator, ubi_4000);
        // investor1 can burn on creator's behalf --> balances = [creator: 199999999999999999996000, investor1:0]

        StorageReference creator_balance = (StorageReference) runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.ofNonVoid(EXCB, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 199999999999999999996000
        BooleanValue equals_result1 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), creator_balance, ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18-4000) = true

        StorageReference investor1_balance = (StorageReference) runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.ofNonVoid(EXCB, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, investor1);
        // investor1_balance = balances[investor1] = 0
        BooleanValue equals_result2 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), investor1_balance, ubi_0);
        // equals_result2 = investor1_balance.equals(0) = true

        StorageReference ubi_remaining_allowance = (StorageReference) runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.ofNonVoid(EXCB, "allowance", UBI, StorageTypes.CONTRACT, StorageTypes.CONTRACT), example_token, creator, investor1);
        // ubi_remaining_allowance = allowances[creator[investor1]] = 7000 - 4000 (just burned) = 3000
        BooleanValue equals_result3 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), ubi_remaining_allowance, ubi_3000);
        // equals_result3 = ubi_remaining_allowance.equals(3000) = true

        StorageReference supply = (StorageReference) runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.ofNonVoid(EXCB, "totalSupply", UBI), example_token);
        // supply = example_token.totalSupply() == 200'000*10^18 - 4000 = 199999999999999999996000
        BooleanValue equals_result4 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), supply, ubi_check);
        // equals_result2 = supply.equals(200'000*10^18 - 4000) = true

        assertTrue(approve_result.getValue());
        assertTrue(equals_result1.getValue() && equals_result2.getValue() && equals_result3.getValue() && equals_result4.getValue());
    }

    @Test
    @DisplayName("Test of ERC20Burnable burnFrom method with the generation of some Exceptions")
    void burnFromException() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCB);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("200000000000000000000000"));
        StorageReference ubi_7000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("7000"));
        StorageReference ubi_8000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("8000"));
        StorageReference ubi_4000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("4000"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("0"));

        BooleanValue approve_result = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCB, "approve", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_7000);
        // Now investor1 is able to spend or burn 7000 MiniEb for creator

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                        addInstanceVoidMethodCallTransaction(
                                investor2_prv_key, investor2,
                                _100_000, panarea(1), jar(),
                                MethodSignatures.ofVoid(EXCB, "burnFrom", StorageTypes.CONTRACT, UBI),
                                example_token,
                                creator, ubi_4000)
                // investor2 cannot burn tokens on creator's behalf --> Exception !!!
        );

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
        				addInstanceVoidMethodCallTransaction(
                                investor1_prv_key, investor1,
                                _100_000, panarea(1), jar(),
                                MethodSignatures.ofVoid(EXCB, "burnFrom", StorageTypes.CONTRACT, UBI),
                                example_token,
                                creator, ubi_8000)
                // investor1 can burn on creator's behalf, but only 7000 token --> Exception !!!
        );

        StorageReference creator_balance = (StorageReference) runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.ofNonVoid(EXCB, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 200000000000000000000000
        BooleanValue equals_result1 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), creator_balance, ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18) = true

        StorageReference investor1_balance = (StorageReference) runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.ofNonVoid(EXCB, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, investor1);
        // investor1_balance = balances[investor1] = 0
        BooleanValue equals_result2 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), investor1_balance, ubi_0);
        // equals_result2 = investor1_balance.equals(0) = true

        StorageReference investor2_balance = (StorageReference) runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.ofNonVoid(EXCB, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, investor2);
        // investor2_balance = balances[investor2] = 0
        BooleanValue equals_result3 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), investor2_balance, ubi_0);
        // equals_result3 = investor2_balance.equals(0) = true

        assertTrue(approve_result.getValue());
        assertTrue(equals_result1.getValue() && equals_result2.getValue() && equals_result3.getValue());
    }
}