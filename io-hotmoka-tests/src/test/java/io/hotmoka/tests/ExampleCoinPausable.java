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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.PrivateKey;

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
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.StorageReference;
import io.takamaka.code.constants.Constants;

/**
 * A test for the ExampleCoinPausable contract (a ERC20Pausable contract).
 */
class ExampleCoinPausable extends HotmokaTest {
    private static final ClassType EXCP = StorageTypes.classNamed("io.hotmoka.examples.tokens.ExampleCoinPausable");
    private static final ClassType UBI = StorageTypes.UNSIGNED_BIG_INTEGER;
    private static final ConstructorSignature CONSTRUCTOR_EXCP = ConstructorSignatures.of(EXCP);
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
        classpath_takamaka_code = takamakaCode();
    }

    @Test @DisplayName("new ExampleCoinPausable()")
    void createExampleCoinCapped() throws Exception {
        addConstructorCallTransaction(
                creator_prv_key, // an object that signs with the payer's private key
                creator, // payer of the transaction
                _500_000, // gas provided to the transaction
                panarea(1), // gas price
                jar(), //reference to the jar being tested
                CONSTRUCTOR_EXCP // constructor signature
        );
    }

    @Test @DisplayName("Test of ERC20Pausable paused method: example_token.paused()")
    void paused() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);

        BooleanValue paused = (BooleanValue) runInstanceNonVoidMethodCallTransaction(
                investor1,
                _100_000, jar(),
                MethodSignatures.ofNonVoid(EXCP, "paused", BOOLEAN),
                example_token);
        // paused = example_token.paused() == false

        assertFalse(paused.getValue());
    }

    @Test @DisplayName("Test of ERC20Pausable _pause method: example_token.pause(...)")
    void _pause() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);

        addInstanceVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                MethodSignatures.ofVoid(EXCP, "pause"),
                example_token);
        // The contract has been put in the paused state

        BooleanValue paused = (BooleanValue) runInstanceNonVoidMethodCallTransaction(
                investor2,
                _100_000, jar(),
                MethodSignatures.ofNonVoid(EXCP, "paused", BOOLEAN),
                example_token);
        // paused = example_token.paused() == true

        assertTrue(paused.getValue());
    }

    @Test @DisplayName("Test of ERC20Pausable _pause method with the generation of an Exception")
    void _pauseException() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);

        addInstanceVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                MethodSignatures.ofVoid(EXCP, "pause"),
                example_token);
        // The contract has been put in the paused state

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceVoidMethodCallTransaction(
                        creator_prv_key, creator,
                        _100_000, panarea(1), jar(),
                        MethodSignatures.ofVoid(EXCP, "pause"),
                        example_token)
                // The contract cannot be put in the paused state if it was already paused --> Exception !!!
        );
    }

    @Test @DisplayName("Test of ERC20Pausable _unpause method: example_token.unpause(...)")
    void _unpause() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);

        addInstanceVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                MethodSignatures.ofVoid(EXCP, "pause"),
                example_token);
        // The contract has been put in the paused state

        BooleanValue paused_before = (BooleanValue) runInstanceNonVoidMethodCallTransaction(
                investor2,
                _100_000, jar(),
                MethodSignatures.ofNonVoid(EXCP, "paused", BOOLEAN),
                example_token);
        // paused_before = example_token.paused() == true

        addInstanceVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                MethodSignatures.ofVoid(EXCP, "unpause"),
                example_token);
        // The contract has been removed from the paused state

        BooleanValue paused_after = (BooleanValue) runInstanceNonVoidMethodCallTransaction(
                investor2,
                _100_000, jar(),
                MethodSignatures.ofNonVoid(EXCP, "paused", BOOLEAN),
                example_token);
        // paused_after = example_token.paused() == false

        assertTrue(paused_before.getValue() && !paused_after.getValue());
    }

    @Test @DisplayName("Test of ERC20Pausable _unpause method with the generation of an Exception")
    void _unpauseException() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                        addInstanceVoidMethodCallTransaction(
                                creator_prv_key, creator,
                                _100_000, panarea(1), jar(),
                                MethodSignatures.ofVoid(EXCP, "unpause"),
                                example_token)
                // The contract cannot be removed from the paused state if it was not paused --> Exception !!!
        );
    }

    @Test @DisplayName("Test of ERC20 transfer method when the contract is not in the paused state")
    void transfer() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("199999999999999999995000"));
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("5000"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("0"));

        BooleanValue transfer_result = (BooleanValue) addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                MethodSignatures.ofNonVoid(EXCP, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token, investor1, ubi_5000);
        // balances = [creator:199999999999999999995000, investor1:500, investor2:0]

        var balanceOf = MethodSignatures.ofNonVoid(EXCP, "balanceOf", UBI, StorageTypes.CONTRACT);

        var creator_balance = runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), balanceOf, example_token, creator).asReturnedReference(balanceOf, NodeException::new);
        // creator_balance = balances[creator] = 199999999999999999995000
        BooleanValue equals_result1 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath_takamaka_code,
                MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                creator_balance,
                ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18-5000) = true

        var investor1_balance = runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), balanceOf, example_token, investor1).asReturnedReference(balanceOf, NodeException::new);
        // investor1_balance = balances[investor1] = 5000
        BooleanValue equals_result2 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath_takamaka_code,
                MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                investor1_balance,
                ubi_5000);
        // equals_result2 = investor1_balance.equals(5000) = true

        var investor2_balance = runInstanceNonVoidMethodCallTransaction(creator, _100_000, jar(), balanceOf, example_token, investor2).asReturnedReference(balanceOf, NodeException::new);
        // investor2_balance = balances[investor2] = 0
        BooleanValue equals_result3 = (BooleanValue) runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath_takamaka_code,
                MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                investor2_balance,
                ubi_0);
        // equals_result3 = investor2_balance.equals(0) = true

        assertTrue(transfer_result.getValue() && equals_result1.getValue() && equals_result2.getValue() && equals_result3.getValue());
    }

    @Test @DisplayName("Test of ERC20 transfer method with the generation of an Exception when the contract is in the paused state")
    void transferException() throws Exception {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("5000"));

        addInstanceVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                MethodSignatures.ofVoid(EXCP, "pause"),
                example_token);
        // The contract has been put in the paused state

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                        addInstanceNonVoidMethodCallTransaction(
                                creator_prv_key, creator,
                                _100_000, panarea(1), jar(),
                                MethodSignatures.ofNonVoid(EXCP, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                                example_token, investor1, ubi_5000)
                // token transfers cannot be made when the contract is paused state --> Exception !!!
        );
    }
}