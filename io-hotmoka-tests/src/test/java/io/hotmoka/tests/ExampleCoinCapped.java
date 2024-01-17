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

import static io.hotmoka.beans.StorageTypes.BOOLEAN;
import static io.hotmoka.helpers.Coin.filicudi;
import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.helpers.Coin.stromboli;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.constants.Constants;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * A test for the ExampleCoinCapped contract (a ERC20Capped contract).
 */
class ExampleCoinCapped extends HotmokaTest {
    private static final ClassType EXCC = StorageTypes.classNamed("io.hotmoka.examples.tokens.ExampleCoinCapped");
    private static final ClassType UBI = StorageTypes.UNSIGNED_BIG_INTEGER;
    private static final ConstructorSignature CONSTRUCTOR_EXCC = ConstructorSignatures.of(EXCC);
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

    @BeforeAll
	static void beforeAll() throws Exception {
		setJar("tokens.jar");
	}

    @BeforeEach
    void beforeEach() throws Exception {
        setAccounts(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        creator = account(1);
        creator_prv_key = privateKey(1);
        classpath_takamaka_code = takamakaCode();
    }

    @Test @DisplayName("new ExampleCoinCapped()")
    void createExampleCoinCapped() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        addConstructorCallTransaction(
                creator_prv_key, // an object that signs with the payer's private key
                creator, // payer of the transaction
                _500_000, // gas provided to the transaction
                panarea(1), // gas price
                jar(), //reference to the jar being tested
                CONSTRUCTOR_EXCC // constructor signature
                );
    }

    @Test @DisplayName("Test of ERC20Capped cap method: example_token.cap() == 1'000'000*10^18")
    void totalSupply() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCC);
        StorageReference ubi_1M = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1000000000000000000000000"));

        StorageReference cap = (StorageReference) runInstanceMethodCallTransaction(
                creator,
                _100_000, jar(),
                MethodSignatures.of(EXCC, "cap", UBI),
                example_token);
        // cap = example_token.cap() == 1'000'000*10^18

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _100_000, classpath_takamaka_code,
                MethodSignatures.of(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                cap, ubi_1M);
        // equals_result = cap.equals(1'000'000*10^18) = true

        assertTrue(equals_result.getValue());
    }

    @Test
    @DisplayName("Test of ERC20Capped _mint method (under the cap): example_token.mint(account, 700'000) --> totalSupply+=700'000, balances[account]+=700'000")
    void mint() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCC);
        StorageReference ubi_700000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("700000000000000000000000"));
        StorageReference ubi_900000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("900000000000000000000000"));

        addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                MethodSignatures.ofVoid(EXCC, "mint", StorageTypes.CONTRACT, UBI),
                example_token,
                creator, ubi_700000);
        // balances = [creator:900000000000000000000000], totalSupply:900000000000000000000000

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.of(EXCC, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 900000000000000000000000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.of(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), creator_balance, ubi_900000);
        // equals_result1 = creator_balance.equals(900'000*10^18) = true

        StorageReference supply = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.of(EXCC, "totalSupply", UBI), example_token);
        // supply = example_token.totalSupply() == 900'000*10^18
        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.of(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), supply, ubi_900000);
        // equals_result2 = supply.equals(900'000*10^18) = true

        assertTrue(equals_result1.getValue() && equals_result2.getValue());
    }

    @Test @DisplayName("Test of ERC20Capped _mint method with the generation of some Exceptions (over the cap)")
    void mintExceptions() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCC);
        StorageReference ubi_200000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("200000000000000000000000"));
        StorageReference ubi_800000_1 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("800000000000000000000001"));
        StorageReference ubi_900000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("900000000000000000000000"));

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceMethodCallTransaction(
                        creator_prv_key, creator,
                        _100_000, panarea(1), jar(),
                        MethodSignatures.ofVoid(EXCC, "mint", StorageTypes.CONTRACT, UBI),
                        example_token,
                        creator, ubi_800000_1)
                // creator cannot mine if the total supply exceeds the cap --> Exception !!!
        );

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceMethodCallTransaction(
                        creator_prv_key, creator,
                        _100_000, panarea(1), jar(),
                        MethodSignatures.ofVoid(EXCC, "mint", StorageTypes.CONTRACT, UBI),
                        example_token,
                        creator, ubi_900000)
                // creator cannot mine if the total supply exceeds the cap --> Exception !!!
        );

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.of(EXCC, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 200000000000000000000000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.of(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), creator_balance, ubi_200000);
        // equals_result1 = creator_balance.equals(200'000*10^18) = true

        StorageReference supply = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), MethodSignatures.of(EXCC, "totalSupply", UBI), example_token);
        // supply = example_token.totalSupply() == 200'000*10^18
        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, MethodSignatures.of(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), supply, ubi_200000);
        // equals_result2 = supply.equals(200'000*10^18) = true

        assertTrue(equals_result1.getValue() && equals_result2.getValue());
    }
}