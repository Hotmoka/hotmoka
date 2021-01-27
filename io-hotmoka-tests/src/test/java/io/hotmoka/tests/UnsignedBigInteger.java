/**
 *
 */
package io.hotmoka.tests;

import static io.hotmoka.beans.Coin.filicudi;
import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.Coin.stromboli;
import static io.hotmoka.beans.types.BasicTypes.*;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.takamaka.code.constants.Constants;

/**
 * A test for the UnsignedBigInteger contract.
 */
class UnsignedBigInteger extends TakamakaTest {
    private static final ClassType UBI = new ClassType("io.takamaka.code.math.UnsignedBigInteger");
    private static final ConstructorSignature CONSTRUCTOR_UBI_BI = new ConstructorSignature(UBI, ClassType.BIG_INTEGER);
    private static final ConstructorSignature CONSTRUCTOR_UBI_STR = new ConstructorSignature(UBI, ClassType.STRING);
    private static final ConstructorSignature CONSTRUCTOR_UBI_STR_INT = new ConstructorSignature(UBI, ClassType.STRING, INT);
    private static final BigInteger _200_000 = BigInteger.valueOf(200_000);

    /**
     * The classpath of the classes being tested.
     */
    private TransactionReference classpath;

    /**
     * The creator of the contract and his private key.
     */
    private StorageReference creator;
    private PrivateKey creator_prv_key;

    @BeforeEach
    void beforeEach() throws Exception {
        setNode(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        classpath = takamakaCode();
        creator = account(1);
        creator_prv_key = privateKey(1);
    }

    @Test @DisplayName("new UnsignedBigInteger(...) with three types of constructor")
    void createUBI() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // UnsignedBigInteger( BigInteger=0 )
        addConstructorCallTransaction(
                creator_prv_key, // an object that signs with the payer's private key
                creator, // payer of the transaction
                _200_000, // gas provided to the transaction
                panarea(1), // gas price
                classpath, //reference to the classpath of the classes being tested
                CONSTRUCTOR_UBI_BI, // constructor signature
                new BigIntegerValue(ZERO)); // actual arguments

        // UnsignedBigInteger( String=10 )
        addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("10"));

        // UnsignedBigInteger( String=20, int radix=10 )
        addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR_INT, new StringValue("20"), new IntValue(10));

        // UnsignedBigInteger( BigInteger=-10 ) --> Exception
        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addConstructorCallTransaction(
                        creator_prv_key, creator,
                        _200_000, panarea(1), classpath,
                        CONSTRUCTOR_UBI_BI,
                        new BigIntegerValue(new BigInteger("-10")))
        );
    }

    @Test @DisplayName("Test of add method (and equals method): 100.add(11).equals(111) == true")
    void add() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_100 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("100"));
        StorageReference ubi_11 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("11"));
        StorageReference ubi_111 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("111"));

        StorageReference ubi_sum = (StorageReference) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(UBI, "add", UBI, UBI),
                ubi_100, ubi_11);
        // ubi_sum = 100.add(11) = 111

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_sum, ubi_111);
        // equals_result = ubi_sum.equals(111) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of subtract method: 100.subtract(1).equals(99) == true")
    void subtract() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_100 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("100"));
        StorageReference ubi_1 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("1"));
        StorageReference ubi_99 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("99"));

        StorageReference ubi_sub = (StorageReference) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(UBI, "subtract", UBI, UBI),
                ubi_100, ubi_1);
        // ubi_sub = 100.subtract(1) = 99

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_sub, ubi_99);
        // equals_result = ubi_sub.equals(99) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of subtract method with the generation of an Exception: 100.subtract(101, 'Test Exception')")
    void subtractException() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_100 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("100"));
        StorageReference ubi_101 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("101"));

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
            addInstanceMethodCallTransaction(
                    creator_prv_key, creator,
                    _200_000, panarea(1), classpath,
                    new NonVoidMethodSignature(UBI, "subtract", UBI, UBI, ClassType.STRING),
                    ubi_100, ubi_101, new StringValue("Test Exception"))
            // 100.subtract(101, 'Test Exception') = 'Test Exception' !!!
        );
    }

    @Test @DisplayName("Test of multiply method: 100.multiply(9).equals(900) == true")
    void multiply() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_100 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("100"));
        StorageReference ubi_9 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("9"));
        StorageReference ubi_900 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("900"));

        StorageReference ubi_mul = (StorageReference) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(UBI, "multiply", UBI, UBI),
                ubi_100, ubi_9);
        // ubi_mul = 100.multiply(9) = 900

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_mul, ubi_900);
        // equals_result = ubi_mul.equals(900) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of divide method: 900.divide(8).equals(112) == true ")
    void divideApproximation() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_900 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("900"));
        StorageReference ubi_8 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("8"));
        StorageReference ubi_112 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("112"));

        StorageReference ubi_div = (StorageReference) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(UBI, "divide", UBI, UBI),
                ubi_900, ubi_8);
        // ubi_div = 900.divide(8) = 112,5 --> 112 (perfectly matches division with uint256 on Solidity)

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_div, ubi_112);
        // equals_result = ubi_div.equals(112) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of divide method: 900.divide(11).equals(81) == true ")
    void divideApproximation2() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_900 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("900"));
        StorageReference ubi_11 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("11"));
        StorageReference ubi_81 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("81"));

        StorageReference ubi_div = (StorageReference) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(UBI, "divide", UBI, UBI),
                ubi_900, ubi_11);
        // ubi_div = 900.divide(11) = 81,818181818 --> 81 (perfectly matches division with uint256 on Solidity)

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_div, ubi_81);
        // equals_result = ubi_div.equals(81) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of divide method with the generation of an Exception: 900.divide(0, 'Test Exception /0')")
    void divideException() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_900 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("900"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("0"));

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceMethodCallTransaction(
                        creator_prv_key, creator,
                        _200_000, panarea(1), classpath,
                        new NonVoidMethodSignature(UBI, "divide", UBI, UBI, ClassType.STRING),
                        ubi_900, ubi_0, new StringValue("Test Exception /0"))
                // 900.divide(0, 'Test Exception /0') = 'Test Exception /0' !!!
        );
    }

    @Test @DisplayName("Test of mod method: 800.mod(13).equals(7) == true ")
    void mod() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));
        StorageReference ubi_13 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("13"));
        StorageReference ubi_7 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("7"));

        StorageReference ubi_mod = (StorageReference) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(UBI, "mod", UBI, UBI),
                ubi_800, ubi_13);
        // ubi_mod = 800.mod(13) = 7

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_mod, ubi_7);
        // equals_result = ubi_mod.equals(7) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of mod method with the generation of an Exception: 800.mod(0, 'Test Exception /0')")
    void modException() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("0"));

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                        addInstanceMethodCallTransaction(
                                creator_prv_key, creator,
                                _200_000, panarea(1), classpath,
                                new NonVoidMethodSignature(UBI, "mod", UBI, UBI, ClassType.STRING),
                                ubi_800, ubi_0, new StringValue("Test Exception /0"))
                // 800.mod(0, 'Test Exception /0') = 'Test Exception /0' !!!
        );
    }

    @Test @DisplayName("Test of pow method: 8.pow(7).equals(2097152) == true")
    void pow() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_8 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("8"));
        StorageReference ubi_2097152 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("2097152"));
        IntValue int_2 = new IntValue(7);

        StorageReference ubi_pow = (StorageReference) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(UBI, "pow", UBI, INT),
                ubi_8, int_2);
        // ubi_pow = 8.pow(7) = 2097152

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_pow, ubi_2097152);
        // equals_result = ubi_pow.equals(2097152) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of max method: 800.max(799).equals(800) == true")
    void max() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));
        StorageReference ubi_799 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("799"));
        StorageReference ubi__800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));

        StorageReference ubi_max = (StorageReference) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(UBI, "max", UBI, UBI),
                ubi_800, ubi_799);
        // ubi_max = 800.max(799) = 800

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_max, ubi__800);
        // equals_result = ubi_max.equals(800) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of min method: 800.min(799).equals(799) == true")
    void min() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));
        StorageReference ubi_799 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("799"));
        StorageReference ubi__799 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("799"));

        StorageReference ubi_min = (StorageReference) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(UBI, "min", UBI, UBI),
                ubi_800, ubi_799);
        // ubi_min = 800.min(799) = 799

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_min, ubi__799);
        // equals_result = ubi_min.equals(799) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of compareTo method: 800.compareTo(799) == 1, 799.compareTo(800) == -1, 800.compareTo(800) == 0")
    void compareToTest() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));
        StorageReference ubi_799 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("799"));
        StorageReference ubi__800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));

        IntValue result_compare1 = (IntValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "compareTo", INT, UBI),
                ubi_800, ubi_799);
        // result_compare1 = 800.compareTo(799) = 1

        IntValue result_compare2 = (IntValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "compareTo", INT, UBI),
                ubi_799, ubi_800);
        // result_compare2 = 799.compareTo(800) = -1

        IntValue result_compare3 = (IntValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "compareTo", INT, UBI),
                ubi_800, ubi__800);
        // result_compare3 = 800.compareTo(800') = 0

        assertEquals(result_compare1.value, 1);
        assertEquals(result_compare2.value, -1);
        assertEquals(result_compare3.value, 0);
    }

    @Test @DisplayName("Test of equals method: 800.compareTo(799) == false, 800.compareTo(800) == true")
    void equalsTest() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));
        StorageReference ubi_799 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("799"));
        StorageReference ubi__800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));

        BooleanValue result_equals1 = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_800, ubi_799);
        // result_equals1 = 800.compareTo(799) = false

        BooleanValue result_equals2 = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_800, ubi__800);
        // result_equals2 = 800.compareTo(800') = true

        assertFalse(result_equals1.value);
        assertTrue(result_equals2.value);
    }

    @Test @DisplayName("Test of hashCode method: 800.hashCode == 800.hashCode(), 800.hashCode != 799.hashCode()")
    void hashCodeTest() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));
        StorageReference ubi_799 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("799"));
        StorageReference ubi__800 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("800"));

        IntValue hashcode_ubi1 = (IntValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "hashCode", INT),
                ubi_800);
        // 800.hashCode()

        IntValue hashcode_ubi2 = (IntValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "hashCode", INT),
                ubi_799);
        // 799.hashCode()

        IntValue hashcode_ubi3 = (IntValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "hashCode", INT),
                ubi__800);
        // 800'.hashCode()

        assertEquals(hashcode_ubi1.value, hashcode_ubi3.value); // 800.hashCode == 800'.hashCode()
        assertNotEquals(hashcode_ubi1.value, hashcode_ubi2.value); // 800.hashCode != 799.hashCode()
    }

    @Test @DisplayName("Test of toBigInteger method: 1001.toBigInteger() == BigInteger@1001")
    void toBigIntegerTest() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_1001 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("1001"));

        BigIntegerValue bi1 = (BigIntegerValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "toBigInteger", ClassType.BIG_INTEGER),
                ubi_1001);
        // 1001.toBigInteger()

        assertEquals(bi1.value, new BigInteger("1001")); // 1001.toBigInteger() == BigInteger@1001
    }

    @Test @DisplayName("Test of toString method: 1001.toString() == '1001'")
    void toStringTest() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_1001 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("1001"));

        StringValue string1 = (StringValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "toString", ClassType.STRING),
                ubi_1001);
        // 1001.toString()

        assertEquals(string1.value, "1001"); // 1001.toString() == '1001'
    }

    @Test @DisplayName("Test of toString method: 1001.toString(16) == '3e9'")
    void toString2Test() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_1001 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("1001"));

        StringValue string2 = (StringValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "toString", ClassType.STRING, INT),
                ubi_1001, new IntValue(16));
        // 1001.toString(16)

        assertEquals(string2.value, "3e9"); // 1001.toString(16) == '3e9'
    }

    @Test @DisplayName("Test of valueOf method: long@99.valueOf().equals(99) == true")
    void valueOfTest() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference ubi_99 = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, new StringValue("99"));

        StorageReference ubi_result = (StorageReference) addStaticMethodCallTransaction(
                creator_prv_key, creator,
                _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(UBI, "valueOf", UBI, LONG),
                new LongValue(99));
        // ubi_result = long@99.valueOf() = 99

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _200_000, classpath,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, ClassType.OBJECT),
                ubi_result, ubi_99);
        // equals_result = ubi_result.equals(99) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of valueOf method with the generation of an Exception: long@-99.valueOf()")
    void valueOfExceptionTest() {
        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addStaticMethodCallTransaction(
                        creator_prv_key, creator,
                        _200_000, panarea(1), classpath,
                        new NonVoidMethodSignature(UBI, "valueOf", UBI, LONG),
                        new LongValue(-99))
                // long@-99.valueOf() = Exception !!!
        );
    }
}