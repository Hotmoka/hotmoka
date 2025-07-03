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
import static io.hotmoka.node.StorageTypes.INT;
import static io.hotmoka.node.StorageTypes.LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.PrivateKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.UnexpectedValueException;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;
import io.takamaka.code.constants.Constants;

/**
 * A test for the UnsignedBigInteger contract.
 */
class UnsignedBigInteger extends HotmokaTest {
    private static final ClassType UBI = StorageTypes.UNSIGNED_BIG_INTEGER;
    private static final ConstructorSignature CONSTRUCTOR_UBI_BI = ConstructorSignatures.of(UBI, StorageTypes.BIG_INTEGER);
    private static final ConstructorSignature CONSTRUCTOR_UBI_STR = ConstructorSignatures.of(UBI, StorageTypes.STRING);
    private static final NonVoidMethodSignature EQUALS = MethodSignatures.ofNonVoid(UBI, "equals", BOOLEAN, StorageTypes.OBJECT);

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
        setAccounts(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        classpath = takamakaCode();
        creator = account(1);
        creator_prv_key = privateKey(1);
    }

    @Test @DisplayName("new UnsignedBigInteger(...) with three types of constructor")
    void createUBI() throws Exception {
        // UnsignedBigInteger( BigInteger=0 )
        addConstructorCallTransaction(
                creator_prv_key, // an object that signs with the payer's private key
                creator, // payer of the transaction
                _100_000, // gas provided to the transaction
                panarea(1), // gas price
                classpath, //reference to the classpath of the classes being tested
                CONSTRUCTOR_UBI_BI, // constructor signature
                StorageValues.bigIntegerOf(0)); // actual arguments

        // UnsignedBigInteger( String=10 )
        addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("10"));

        // UnsignedBigInteger( BigInteger=-10 ) --> Exception
        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
        	addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_BI, StorageValues.bigIntegerOf(-10))
        );
    }

    @Test @DisplayName("Test of add method (and equals method): 100.add(11).equals(111) == true")
    void add() throws Exception {
        StorageReference ubi_100 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("100"));
        StorageReference ubi_11 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("11"));
        StorageReference ubi_111 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("111"));

        var add = MethodSignatures.ofNonVoid(UBI, "add", UBI, UBI);
        var ubi_sum = addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), classpath,
                add,
                ubi_100, ubi_11).asReturnedReference(add, UnexpectedValueException::new);
        // ubi_sum = 100.add(11) = 111

		var equals_result = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_sum, ubi_111).asReturnedBoolean(EQUALS, UnexpectedValueException::new);
        // equals_result = ubi_sum.equals(111) = true

        assertTrue(equals_result);
    }

    @Test @DisplayName("Test of subtract method: 100.subtract(1).equals(99) == true")
    void subtract() throws Exception {
        StorageReference ubi_100 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("100"));
        StorageReference ubi_1 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1"));
        StorageReference ubi_99 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("99"));

        var subtract = MethodSignatures.ofNonVoid(UBI, "subtract", UBI, UBI);
		StorageReference ubi_sub = addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), classpath,
                subtract,
                ubi_100, ubi_1).asReturnedReference(subtract, UnexpectedValueException::new);
        // ubi_sub = 100.subtract(1) = 99

		boolean equals_result = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_sub, ubi_99).asReturnedBoolean(EQUALS, UnexpectedValueException::new);
        // equals_result = ubi_sub.equals(99) = true

        assertTrue(equals_result);
    }

    @Test @DisplayName("Test of subtract method with the generation of an Exception: 100.subtract(101, 'Test Exception')")
    void subtractException() throws Exception {
        StorageReference ubi_100 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("100"));
        StorageReference ubi_101 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("101"));

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
            addInstanceNonVoidMethodCallTransaction(
                    creator_prv_key, creator,
                    _100_000, panarea(1), classpath,
                    MethodSignatures.ofNonVoid(UBI, "subtract", UBI, UBI, StorageTypes.STRING),
                    ubi_100, ubi_101, StorageValues.stringOf("Test Exception"))
            // 100.subtract(101, 'Test Exception') = 'Test Exception' !!!
        );
    }

    @Test @DisplayName("Test of multiply method: 100.multiply(9).equals(900) == true")
    void multiply() throws Exception {
        StorageReference ubi_100 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("100"));
        StorageReference ubi_9 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("9"));
        StorageReference ubi_900 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("900"));

        var multiply = MethodSignatures.ofNonVoid(UBI, "multiply", UBI, UBI);
		StorageReference ubi_mul = addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), classpath,
                multiply,
                ubi_100, ubi_9).asReturnedReference(multiply, UnexpectedValueException::new);
        // ubi_mul = 100.multiply(9) = 900

        boolean equals_result = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_mul, ubi_900).asReturnedBoolean(EQUALS, UnexpectedValueException::new);
        // equals_result = ubi_mul.equals(900) = true

        assertTrue(equals_result);
    }

    @Test @DisplayName("Test of divide method: 900.divide(8).equals(112) == true ")
    void divideApproximation() throws Exception {
        StorageReference ubi_900 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("900"));
        StorageReference ubi_8 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("8"));
        StorageReference ubi_112 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("112"));

        var divide = MethodSignatures.ofNonVoid(UBI, "divide", UBI, UBI);
		StorageReference ubi_div = addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), classpath,
                divide,
                ubi_900, ubi_8).asReturnedReference(divide, UnexpectedValueException::new);
        // ubi_div = 900.divide(8) = 112,5 --> 112 (perfectly matches division with uint256 on Solidity)

		boolean equals_result = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_div, ubi_112).asReturnedBoolean(EQUALS, UnexpectedValueException::new);
        // equals_result = ubi_div.equals(112) = true

        assertTrue(equals_result);
    }

    @Test @DisplayName("Test of divide method: 900.divide(11).equals(81) == true ")
    void divideApproximation2() throws Exception {
        StorageReference ubi_900 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("900"));
        StorageReference ubi_11 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("11"));
        StorageReference ubi_81 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("81"));

        var divide = MethodSignatures.ofNonVoid(UBI, "divide", UBI, UBI);
		var ubi_div = addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), classpath,
                divide,
                ubi_900, ubi_11).asReturnedReference(divide, UnexpectedValueException::new);
        // ubi_div = 900.divide(11) = 81,818181818 --> 81 (perfectly matches division with uint256 on Solidity)

        boolean equals_result = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_div, ubi_81).asReturnedBoolean(EQUALS, UnexpectedValueException::new);
        // equals_result = ubi_div.equals(81) = true

        assertTrue(equals_result);
    }

    @Test @DisplayName("Test of divide method with the generation of an Exception: 900.divide(0, 'Test Exception /0')")
    void divideException() throws Exception {
        StorageReference ubi_900 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("900"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("0"));

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceNonVoidMethodCallTransaction(
                        creator_prv_key, creator,
                        _100_000, panarea(1), classpath,
                        MethodSignatures.ofNonVoid(UBI, "divide", UBI, UBI, StorageTypes.STRING),
                        ubi_900, ubi_0, StorageValues.stringOf("Test Exception /0"))
                // 900.divide(0, 'Test Exception /0') = 'Test Exception /0' !!!
        );
    }

    @Test @DisplayName("Test of mod method: 800.mod(13).equals(7) == true ")
    void mod() throws Exception {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("800"));
        StorageReference ubi_13 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("13"));
        StorageReference ubi_7 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("7"));

        var mod = MethodSignatures.ofNonVoid(UBI, "mod", UBI, UBI);
		StorageReference ubi_mod = addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), classpath,
                mod,
                ubi_800, ubi_13).asReturnedReference(mod, UnexpectedValueException::new);
        // ubi_mod = 800.mod(13) = 7

        boolean equals_result = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_mod, ubi_7).asReturnedBoolean(EQUALS, UnexpectedValueException::new);
        // equals_result = ubi_mod.equals(7) = true

        assertTrue(equals_result);
    }

    @Test @DisplayName("Test of mod method with the generation of an Exception: 800.mod(0, 'Test Exception /0')")
    void modException() throws Exception {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("800"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("0"));

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                        addInstanceNonVoidMethodCallTransaction(
                                creator_prv_key, creator,
                                _100_000, panarea(1), classpath,
                                MethodSignatures.ofNonVoid(UBI, "mod", UBI, UBI, StorageTypes.STRING),
                                ubi_800, ubi_0, StorageValues.stringOf("Test Exception /0"))
                // 800.mod(0, 'Test Exception /0') = 'Test Exception /0' !!!
        );
    }

    @Test @DisplayName("Test of pow method: 8.pow(7).equals(2097152) == true")
    void pow() throws Exception {
        StorageReference ubi_8 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("8"));
        StorageReference ubi_2097152 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("2097152"));
        IntValue int_2 = StorageValues.intOf(7);

        var pow = MethodSignatures.ofNonVoid(UBI, "pow", UBI, INT);
		var ubi_pow = addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), classpath,
                pow,
                ubi_8, int_2).asReturnedReference(pow, UnexpectedValueException::new);
        // ubi_pow = 8.pow(7) = 2097152

        boolean equals_result = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_pow, ubi_2097152).asReturnedBoolean(EQUALS, UnexpectedValueException::new);
        // equals_result = ubi_pow.equals(2097152) = true

        assertTrue(equals_result);
    }

    @Test @DisplayName("Test of max method: 800.max(799).equals(800) == true")
    void max() throws Exception {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("800"));
        StorageReference ubi_799 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("799"));
        StorageReference ubi__800 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("800"));

        var max = MethodSignatures.ofNonVoid(UBI, "max", UBI, UBI);
		StorageReference ubi_max = addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), classpath,
                max,
                ubi_800, ubi_799).asReturnedReference(max, UnexpectedValueException::new);
        // ubi_max = 800.max(799) = 800

        boolean equals_result = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_max, ubi__800).asReturnedBoolean(EQUALS, UnexpectedValueException::new);
        // equals_result = ubi_max.equals(800) = true

        assertTrue(equals_result);
    }

    @Test @DisplayName("Test of min method: 800.min(799).equals(799) == true")
    void min() throws Exception {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("800"));
        StorageReference ubi_799 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("799"));
        StorageReference ubi__799 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("799"));

        var min = MethodSignatures.ofNonVoid(UBI, "min", UBI, UBI);
		StorageReference ubi_min = addInstanceNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), classpath,
                min,
                ubi_800, ubi_799).asReturnedReference(min, UnexpectedValueException::new);
        // ubi_min = 800.min(799) = 799

        boolean equals_result = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_min, ubi__799).asReturnedBoolean(EQUALS, UnexpectedValueException::new);
        // equals_result = ubi_min.equals(799) = true

        assertTrue(equals_result);
    }

    @Test @DisplayName("Test of compareTo method: 800.compareTo(799) == 1, 799.compareTo(800) == -1, 800.compareTo(800) == 0")
    void compareToTest() throws Exception {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("800"));
        StorageReference ubi_799 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("799"));
        StorageReference ubi__800 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("800"));

        var compareTo = MethodSignatures.ofNonVoid(UBI, "compareTo", INT, UBI);
        int result_compare1 = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                compareTo,
                ubi_800, ubi_799).asReturnedInt(compareTo, UnexpectedValueException::new);
        // result_compare1 = 800.compareTo(799) = 1

        int result_compare2 = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                compareTo,
                ubi_799, ubi_800).asReturnedInt(compareTo, UnexpectedValueException::new);
        // result_compare2 = 799.compareTo(800) = -1

        int result_compare3 = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                compareTo,
                ubi_800, ubi__800).asReturnedInt(compareTo, UnexpectedValueException::new);
        // result_compare3 = 800.compareTo(800') = 0

        assertEquals(result_compare1, 1);
        assertEquals(result_compare2, -1);
        assertEquals(result_compare3, 0);
    }

    @Test @DisplayName("Test of equals method: 800.compareTo(799) == false, 800.compareTo(800) == true")
    void equalsTest() throws Exception {
        StorageReference ubi_800 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("800"));
        StorageReference ubi_799 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("799"));
        StorageReference ubi__800 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("800"));

        boolean result_equals1 = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_800, ubi_799).asReturnedBoolean(EQUALS, UnexpectedValueException::new);;
        // result_equals1 = 800.compareTo(799) = false

        boolean result_equals2 = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_800, ubi__800).asReturnedBoolean(EQUALS, UnexpectedValueException::new);
        // result_equals2 = 800.compareTo(800') = true

        assertFalse(result_equals1);
        assertTrue(result_equals2);
    }

    @Test @DisplayName("Test of toBigInteger method: 1001.toBigInteger() == BigInteger@1001")
    void toBigIntegerTest() throws Exception {
        StorageReference ubi_1001 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1001"));

        var toBigInteger = MethodSignatures.ofNonVoid(UBI, "toBigInteger", StorageTypes.BIG_INTEGER);
		BigInteger bi1 = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                toBigInteger,
                ubi_1001).asReturnedBigInteger(toBigInteger, UnexpectedValueException::new);
        // 1001.toBigInteger()

        assertEquals(bi1, BigInteger.valueOf(1001)); // 1001.toBigInteger() == BigInteger@1001
    }

    @Test @DisplayName("Test of toString method: 1001.toString() == '1001'")
    void toStringTest() throws Exception {
        StorageReference ubi_1001 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("1001"));

        var toString = MethodSignatures.ofNonVoid(UBI, "toString", StorageTypes.STRING);
		String string1 = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                toString,
                ubi_1001).asReturnedString(toString, UnexpectedValueException::new);
        // 1001.toString()

        assertEquals(string1, "1001"); // 1001.toString() == '1001'
    }

    @Test @DisplayName("Test of valueOf method: long@99.valueOf().equals(99) == true")
    void valueOfTest() throws Exception {
        StorageReference ubi_99 = addConstructorCallTransaction(creator_prv_key, creator, _100_000, panarea(1), classpath, CONSTRUCTOR_UBI_STR, StorageValues.stringOf("99"));

        var valueOf = MethodSignatures.ofNonVoid(UBI, "valueOf", UBI, LONG);
		StorageReference ubi_result = addStaticNonVoidMethodCallTransaction(
                creator_prv_key, creator,
                _100_000, panarea(1), classpath,
                valueOf,
                StorageValues.longOf(99)).asReturnedReference(valueOf, UnexpectedValueException::new);
        // ubi_result = long@99.valueOf() = 99

        boolean equals_result = runInstanceNonVoidMethodCallTransaction(
                creator,
                _100_000, classpath,
                EQUALS,
                ubi_result, ubi_99).asReturnedBoolean(EQUALS, UnexpectedValueException::new);;
        // equals_result = ubi_result.equals(99) = true

        assertTrue(equals_result);
    }

    @Test @DisplayName("Test of valueOf method with the generation of an Exception: long@-99.valueOf()")
    void valueOfExceptionTest() {
        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addStaticNonVoidMethodCallTransaction(
                        creator_prv_key, creator,
                        _100_000, panarea(1), classpath,
                        MethodSignatures.ofNonVoid(UBI, "valueOf", UBI, LONG),
                        StorageValues.longOf(-99))
                // long@-99.valueOf() = Exception !!!
        );
    }
}