/**
 *
 */
package io.hotmoka.tests;

import static io.hotmoka.beans.Coin.filicudi;
import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.Coin.stromboli;
import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * A test for the ExampleCoinPausable contract (a ERC20Pausable contract).
 */
class ExampleCoinPausable extends TakamakaTest {
    private static final ClassType EXCP = new ClassType("io.hotmoka.examples.tokens.ExampleCoinPausable");
    private static final ClassType UBI = ClassType.UNSIGNED_BIG_INTEGER;
    private static final ConstructorSignature CONSTRUCTOR_EXCP = new ConstructorSignature(EXCP);
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
    void createExampleCoinCapped() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
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
    void paused() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);

        BooleanValue paused = (BooleanValue) runInstanceMethodCallTransaction(
                investor1,
                _100_000, jar(),
                new NonVoidMethodSignature(EXCP, "paused", BOOLEAN),
                example_token);
        // paused = example_token.paused() == false

        assertFalse(paused.value);
    }

    @Test @DisplayName("Test of ERC20Pausable _pause method: example_token.pause(...)")
    void _pause() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);

        addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                new VoidMethodSignature(EXCP, "pause"),
                example_token);
        // The contract has been put in the paused state

        BooleanValue paused = (BooleanValue) runInstanceMethodCallTransaction(
                investor2,
                _100_000, jar(),
                new NonVoidMethodSignature(EXCP, "paused", BOOLEAN),
                example_token);
        // paused = example_token.paused() == true

        assertTrue(paused.value);
    }

    @Test @DisplayName("Test of ERC20Pausable _pause method with the generation of an Exception")
    void _pauseException() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);

        addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                new VoidMethodSignature(EXCP, "pause"),
                example_token);
        // The contract has been put in the paused state

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceMethodCallTransaction(
                        creator_prv_key, creator,
                        _100_000, panarea(1), jar(),
                        new VoidMethodSignature(EXCP, "pause"),
                        example_token)
                // The contract cannot be put in the paused state if it was already paused --> Exception !!!
        );
    }

    @Test @DisplayName("Test of ERC20Pausable _unpause method: example_token.unpause(...)")
    void _unpause() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);

        addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                new VoidMethodSignature(EXCP, "pause"),
                example_token);
        // The contract has been put in the paused state

        BooleanValue paused_before = (BooleanValue) runInstanceMethodCallTransaction(
                investor2,
                _100_000, jar(),
                new NonVoidMethodSignature(EXCP, "paused", BOOLEAN),
                example_token);
        // paused_before = example_token.paused() == true

        addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                new VoidMethodSignature(EXCP, "unpause"),
                example_token);
        // The contract has been removed from the paused state

        BooleanValue paused_after = (BooleanValue) runInstanceMethodCallTransaction(
                investor2,
                _100_000, jar(),
                new NonVoidMethodSignature(EXCP, "paused", BOOLEAN),
                example_token);
        // paused_after = example_token.paused() == false

        assertTrue(paused_before.value && !paused_after.value);
    }

    @Test @DisplayName("Test of ERC20Pausable _unpause method with the generation of an Exception")
    void _unpauseException() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                        addInstanceMethodCallTransaction(
                                creator_prv_key, creator,
                                _100_000, panarea(1), jar(),
                                new VoidMethodSignature(EXCP, "unpause"),
                                example_token)
                // The contract cannot be removed from the paused state if it was not paused --> Exception !!!
        );
    }

    @Test @DisplayName("Test of ERC20 transfer method when the contract is not in the paused state")
    void transfer() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("199999999999999999995000"));
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("5000"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("0"));

        BooleanValue transfer_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXCP, "transfer", BOOLEAN, ClassType.CONTRACT, UBI),
                example_token, investor1, ubi_5000);
        // balances = [creator:199999999999999999995000, investor1:500, investor2:0]

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCP, "balanceOf", UBI, ClassType.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 199999999999999999995000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _100_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                creator_balance,
                ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18-5000) = true

        StorageReference investor1_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCP, "balanceOf", UBI, ClassType.CONTRACT), example_token, investor1);
        // investor1_balance = balances[investor1] = 5000
        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _100_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                investor1_balance,
                ubi_5000);
        // equals_result2 = investor1_balance.equals(5000) = true

        StorageReference investor2_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _100_000, jar(), new NonVoidMethodSignature(EXCP, "balanceOf", UBI, ClassType.CONTRACT), example_token, investor2);
        // investor2_balance = balances[investor2] = 0
        BooleanValue equals_result3 = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _100_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                investor2_balance,
                ubi_0);
        // equals_result3 = investor2_balance.equals(0) = true

        assertTrue(transfer_result.value && equals_result1.value && equals_result2.value && equals_result3.value);
    }

    @Test @DisplayName("Test of ERC20 transfer method with the generation of an Exception when the contract is in the paused state")
    void transferException() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXCP);
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("5000"));

        addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), jar(),
                new VoidMethodSignature(EXCP, "pause"),
                example_token);
        // The contract has been put in the paused state

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                        addInstanceMethodCallTransaction(
                                creator_prv_key, creator,
                                _100_000, panarea(1), jar(),
                                new NonVoidMethodSignature(EXCP, "transfer", BOOLEAN, ClassType.CONTRACT, UBI),
                                example_token, investor1, ubi_5000)
                // token transfers cannot be made when the contract is paused state --> Exception !!!
        );
    }
}