/**
 *
 */
package io.hotmoka.tests;

import static io.hotmoka.beans.Coin.filicudi;
import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.Coin.stromboli;
import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.takamaka.code.constants.Constants;

/**
 * A test for the ExampleCoinBurnable contract (a ERC20Burnable contract).
 */
class ExampleCoinBurnable extends TakamakaTest {
    private static final ClassType EXCB = new ClassType("io.hotmoka.tests.tokens.ExampleCoinBurnable");
    private static final ClassType UBI = new ClassType("io.takamaka.code.math.UnsignedBigInteger");
    private static final ConstructorSignature CONSTRUCTOR_EXCB = new ConstructorSignature(EXCB);
    private static final ConstructorSignature CONSTRUCTOR_UBI_STR = new ConstructorSignature(UBI, ClassType.STRING);

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
		setJar("examplecoin.jar");
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
    void createExampleCoinBurnable() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        addConstructorCallTransaction(
                creator_prv_key, // an object that signs with the payer's private key
                creator, // payer of the transaction
                _100_000, // gas provided to the transaction
                panarea(1), // gas price
                jar(), //reference to the jar being tested
                CONSTRUCTOR_EXCB // constructor signature
                );
    }

    @Test
    @DisplayName("Test of ERC20Burnable burn method: example_token.burn(500'000) --> totalSupply-=500'000, balances[caller]-=500'000")
    void burn() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), jar(), CONSTRUCTOR_EXCB);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("199999999999999999500000"));
        StorageReference ubi_500000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("500000"));

        addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                new VoidMethodSignature(EXCB, "burn", UBI),
                example_token,
                ubi_500000);
        // balances = [creator:199999999999999999500000], totalSupply:199999999999999999500000

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCB, "balanceOf", UBI, ClassType.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 199999999999999999500000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT), creator_balance, ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18 - 500000) = true

        StorageReference supply = (StorageReference) runInstanceMethodCallTransaction(
                creator,
                _100_000, jar(),
                new NonVoidMethodSignature(EXCB, "totalSupply", UBI),
                example_token);
        // supply = example_token.totalSupply() == 200'000*10^18 - 500000

        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT), supply, ubi_check);
        // equals_result2 = supply.equals(200'000*10^18 - 500000) = true

        assertTrue(equals_result1.value && equals_result2.value);
    }

    @Test
    @DisplayName("Test of ERC20Burnable burnFrom method: example_token.burnFrom(recipient, 500'000) --> totalSupply-=500'000, balances[recipient]-=500'000")
    void burnFrom() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), jar(), CONSTRUCTOR_EXCB);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("199999999999999999996000"));
        StorageReference ubi_7000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("7000"));
        StorageReference ubi_4000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("4000"));
        StorageReference ubi_3000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("3000"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("0"));

        BooleanValue approve_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXCB, "approve", BOOLEAN, ClassType.CONTRACT, UBI),
                example_token,
                investor1, ubi_7000);
        // Now investor1 is able to spend or burn 7000 MiniEb for creator

        addInstanceMethodCallTransaction(
                investor1_prv_key, investor1,
                _100_000, panarea(1), jar(),
                new VoidMethodSignature(EXCB, "burnFrom", ClassType.CONTRACT, UBI),
                example_token,
                creator, ubi_4000);
        // investor1 can burn on creator's behalf --> balances = [creator: 199999999999999999996000, investor1:0]

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCB, "balanceOf", UBI, ClassType.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 199999999999999999996000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT), creator_balance, ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18-4000) = true

        StorageReference investor1_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCB, "balanceOf", UBI, ClassType.CONTRACT), example_token, investor1);
        // investor1_balance = balances[investor1] = 0
        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT), investor1_balance, ubi_0);
        // equals_result2 = investor1_balance.equals(0) = true

        StorageReference ubi_remaining_allowance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCB, "allowance", UBI, ClassType.CONTRACT, ClassType.CONTRACT), example_token, creator, investor1);
        // ubi_remaining_allowance = allowances[creator[investor1]] = 7000 - 4000 (just burned) = 3000
        BooleanValue equals_result3 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT), ubi_remaining_allowance, ubi_3000);
        // equals_result3 = ubi_remaining_allowance.equals(3000) = true

        StorageReference supply = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCB, "totalSupply", UBI), example_token);
        // supply = example_token.totalSupply() == 200'000*10^18 - 4000 = 199999999999999999996000
        BooleanValue equals_result4 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT), supply, ubi_check);
        // equals_result2 = supply.equals(200'000*10^18 - 4000) = true

        assertTrue(approve_result.value);
        assertTrue(equals_result1.value && equals_result2.value && equals_result3.value && equals_result4.value);
    }

    @Test
    @DisplayName("Test of ERC20Burnable burnFrom method with the generation of some Exceptions")
    void burnFromException() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), jar(), CONSTRUCTOR_EXCB);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("200000000000000000000000"));
        StorageReference ubi_7000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("7000"));
        StorageReference ubi_8000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("8000"));
        StorageReference ubi_4000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("4000"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("0"));

        BooleanValue approve_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXCB, "approve", BOOLEAN, ClassType.CONTRACT, UBI),
                example_token,
                investor1, ubi_7000);
        // Now investor1 is able to spend or burn 7000 MiniEb for creator

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                        addInstanceMethodCallTransaction(
                                investor2_prv_key, investor2,
                                _100_000, panarea(1), jar(),
                                new VoidMethodSignature(EXCB, "burnFrom", ClassType.CONTRACT, UBI),
                                example_token,
                                creator, ubi_4000)
                // investor2 cannot burn tokens on creator's behalf --> Exception !!!
        );

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                        addInstanceMethodCallTransaction(
                                investor1_prv_key, investor1,
                                _100_000, panarea(1), jar(),
                                new VoidMethodSignature(EXCB, "burnFrom", ClassType.CONTRACT, UBI),
                                example_token,
                                creator, ubi_8000)
                // investor1 can burn on creator's behalf, but only 7000 token --> Exception !!!
        );

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCB, "balanceOf", UBI, ClassType.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 200000000000000000000000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT), creator_balance, ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18) = true

        StorageReference investor1_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCB, "balanceOf", UBI, ClassType.CONTRACT), example_token, investor1);
        // investor1_balance = balances[investor1] = 0
        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT), investor1_balance, ubi_0);
        // equals_result2 = investor1_balance.equals(0) = true

        StorageReference investor2_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCB, "balanceOf", UBI, ClassType.CONTRACT), example_token, investor2);
        // investor2_balance = balances[investor2] = 0
        BooleanValue equals_result3 = (BooleanValue) runInstanceMethodCallTransaction(creator, _100_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT), investor2_balance, ubi_0);
        // equals_result3 = investor2_balance.equals(0) = true

        assertTrue(approve_result.value);
        assertTrue(equals_result1.value && equals_result2.value && equals_result3.value);
    }
}