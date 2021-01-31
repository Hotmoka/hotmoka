/**
 *
 */
package io.hotmoka.tests;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import static io.hotmoka.beans.Coin.*;
import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test for the ExampleCoin contract (a ERC20 contract).
 */
class ImpossibleTestUBI extends TakamakaTest {
    private static final ClassType IMPOSSIBLETEST = new ClassType("io.hotmoka.tests.tokens.ImpossibleTestUBI");
    private static final ClassType UBI = new ClassType("io.takamaka.code.math.UnsignedBigInteger");
    private static final ConstructorSignature CONSTRUCTOR_IMPOSSIBLETEST = new ConstructorSignature(IMPOSSIBLETEST);
    private static final ConstructorSignature CONSTRUCTOR_UBI_STR = new ConstructorSignature(UBI, ClassType.STRING);
    private static final BigInteger _200_000 = BigInteger.valueOf(200_000);

    /**
     * The classpath of the classes of code module.
     */
    private TransactionReference classpath_takamaka_code;

    /**
     * The creator of the coin.
     */
    private StorageReference creator;
    private PrivateKey creator_prv_key;

    @BeforeEach
    void beforeEach() throws Exception {
        setNode("examplecoin.jar", stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        creator = account(1);
        creator_prv_key = privateKey(1);
        classpath_takamaka_code = takamakaCode();
    }

    @Test @DisplayName("new ExampleCoin()")
    void createExampleCoin() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        addConstructorCallTransaction(
                creator_prv_key, // an object that signs with the payer's private key
                creator, // payer of the transaction
                _200_000, // gas provided to the transaction
                panarea(1), // gas price
                jar(), //reference to the jar being tested
                CONSTRUCTOR_IMPOSSIBLETEST // constructor signature
                );
    }

    @Test @DisplayName("Test of ERC20 name method: example_token.name() == 'ExampleCoin'")
    void name() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), jar(), CONSTRUCTOR_IMPOSSIBLETEST);

        StorageReference ubi_1 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("1"));

        BooleanValue result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, jar(),
                new NonVoidMethodSignature(IMPOSSIBLETEST, "test", BOOLEAN, UBI),
                example_token,
                ubi_1);

        assertTrue(result.value);
    }

}