package io.takamaka.code.util;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * It represents an Unsigned "java.math.BigInteger" through a simple wrapping.
 * This object has the purpose of representing a certain amount of tokens or any currency and it aims to provide a valid
 * alternative to the "uint256" that exists in Solidity (regarding the use of currencies).
 * Implements most common constructors of BigInteger and some useful function of it.
 */
public class UnsignedBigInteger extends Storage {
    /**
     * Stored value (guaranteed to be >= 0)
     */
    private final BigInteger val;

    /**
     * Creates the UnsignedBigInteger: given in input a java.math.BigInteger verifies that it is zero or a positive
     * number and then accepts it as an UnsignedBigInteger value.
     *
     * @param val the value to allocate (it must be >= 0)
     */
    public UnsignedBigInteger(BigInteger val) {
        require(val.signum() >= 0, "Illegal value: negative value"); // val >= 0
        this.val = val;
    }

    /**
     * Creates the UnsignedBigInteger: given in input a String (representing a number) verifies that it is zero or a
     * positive number and then accepts it as an UnsignedBigInteger value.
     *
     * @param val the value in string format to allocate (it must be >= 0)
     */
    public UnsignedBigInteger(String val) {
        this(new BigInteger(val));
    }

    /**
     * Creates the UnsignedBigInteger: given in input a String (representing a number coded with a specific base)
     * verifies that it is zero or a positive number and then accepts it as an UnsignedBigInteger value.
     *
     * @param val the value in string format to allocate, it is coded with a specific base (it must be >= 0)
     * @param radix the coding base of val, e.g. 2 for binary
     */
    public UnsignedBigInteger(String val, int radix) {
        this(new BigInteger(val, radix));
    }

    /**
     * A constructor for internal use that is equal to UnsignedBigInteger(BigInteger) but it does not verify that
     * "val" >= 0.
     *
     * @param val the value to allocate (it is assumed that it is >= 0)
     * @param dummy disambiguator for the homonymous public constructor
     */
    private UnsignedBigInteger(BigInteger val, boolean dummy){
        this.val = val;
    }

    /**
     * Returns a UnsignedBigInteger whose value is (this + other).
     *
     * @param other value that will be added to this UnsignedBigInteger
     * @return the result of (this + other)
     */
    public UnsignedBigInteger add(UnsignedBigInteger other) {
        return new UnsignedBigInteger(val.add(other.val), true);
    }

    /**
     * Returns a UnsignedBigInteger whose value is (this - other).
     * The result must be positive so this must be >= other: if the condition is violated, an exception is raised
     * with the message "errorMessage".
     *
     * @param other value that will be subtracted from this UnsignedBigInteger (it is required that other <= this)
     * @param errorMessage message returned in case other > this
     * @return the result of (this - other)
     */
    public UnsignedBigInteger subtract(UnsignedBigInteger other, String errorMessage) {
        BigInteger diff = val.subtract(other.val);
        require(diff.signum() >= 0, errorMessage); // this.val >= other.val
        return new UnsignedBigInteger(diff, true);
    }

    /**
     * Returns a UnsignedBigInteger whose value is (this - other).
     * The result must be positive so this must be >= other: if the condition is violated, an exception is raised.
     *
     * @param other value that will be subtracted from this UnsignedBigInteger (it is required that other <= this)
     * @return the result of (this - other)
     */
    public UnsignedBigInteger subtract(UnsignedBigInteger other) {
        return subtract(other, "Illegal operation: subtraction overflow");
    }

    /**
     * Returns a UnsignedBigInteger whose value is (this * other).
     *
     * @param other value that will be multiplied to this UnsignedBigInteger
     * @return the result of (this * other)
     */
    public UnsignedBigInteger multiply(UnsignedBigInteger other) {
        return new UnsignedBigInteger(val.multiply(other.val), true);
    }

    /**
     * Returns a UnsignedBigInteger whose value is (this / other).
     * other cannot be zero: if the condition is violated, an exception is raised with the message "errorMessage".
     *
     * @param other divider value of the operation (it cannot be zero)
     * @param errorMessage message returned in case other is zero
     * @return the integer result of (this / other)
     */
    public UnsignedBigInteger divide(UnsignedBigInteger other, String errorMessage) {
        require(other.val.signum() != 0, errorMessage); // other.val > 0
        return new UnsignedBigInteger(val.divide(other.val), true);
    }

    /**
     * Returns a UnsignedBigInteger whose value is (this / other). other cannot be zero.
     *
     * @param other divider value of the operation (it cannot be zero)
     * @return the integer result of (this / other)
     */
    public UnsignedBigInteger divide(UnsignedBigInteger other) {
        return divide(other, "Illegal operation: division by zero");
    }

    /**
     * Returns a UnsignedBigInteger whose value is (this mod m).
     * m cannot be zero: if the condition is violated, an exception is raised with the message "errorMessage".
     *
     * @param m modulus of the operation (it cannot be zero)
     * @param errorMessage message returned in case m is zero
     * @return the result of (this mod m)
     */
    public UnsignedBigInteger mod(UnsignedBigInteger m, String errorMessage) {
        require(m.val.signum() != 0, errorMessage); // other.val > 0
        return new UnsignedBigInteger(val.mod(m.val), true);
    }

    /**
     * Returns a UnsignedBigInteger whose value is (this mod m). m cannot be zero.
     *
     * @param m modulus of the operation (it cannot be zero)
     * @return the result of (this mod m)
     */
    public UnsignedBigInteger mod(UnsignedBigInteger m) {
        return mod(m, "Illegal operation: modulo by zero");
    }

    /**
     * Returns a UnsignedBigInteger whose value is (this ^ exponent). Note that exponent is an integer rather than a
     * UnsignedBigInteger.
     *
     * @param exponent exponent to which this UnsignedBigInteger is to be raised.
     * @return the result of (this ^ exponent)
     */
    public UnsignedBigInteger pow(int exponent) {
        return new UnsignedBigInteger(val.pow(exponent), true);
    }

    /**
     * Returns the maximum of this UnsignedBigInteger and other.
     *
     * @param other value with which the maximum is to be computed.
     * @return the UnsignedBigInteger whose value is the greater of this and val. If they are equal, either may be
     *         returned.
     */
    public UnsignedBigInteger max(UnsignedBigInteger other) {
        return new UnsignedBigInteger(val.max(other.val), true);
    }

    /**
     * Returns the minimum of this UnsignedBigInteger and other.
     *
     * @param other value with which the minimum is to be computed.
     * @return the UnsignedBigInteger whose value is the lesser of this UnsignedBigInteger and val. If they are equal,
     *         either may be returned.
     */
    public UnsignedBigInteger min(UnsignedBigInteger other) {
        return new UnsignedBigInteger(val.min(other.val), true);
    }

    /**
     * Compares this UnsignedBigInteger with the specified UnsignedBigInteger other.
     *
     * @param other UnsignedBigInteger to which this UnsignedBigInteger is to be compared.
     * @return -1, 0 or 1 as this UnsignedBigInteger is numerically less than, equal to, or greater than other.
     */
    public @View int compareTo(UnsignedBigInteger other) {
        return val.compareTo(other.val);
    }

    /**
     * Compares this UnsignedBigInteger with the specified Object for equality.
     *
     * @param other Object to which this UnsignedBigInteger is to be compared.
     * @return true if and only if the specified Object is a UnsignedBigInteger whose value is numerically equal to this
     * 		   UnsignedBigInteger.
     */
    public @View boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof UnsignedBigInteger))
            return false;
        UnsignedBigInteger otherUBI = (UnsignedBigInteger)other;
        return val.equals(otherUBI.val);
    }

    /**
     * Returns the hash code for this UnsignedBigInteger.
     *
     * @return hash code for this UnsignedBigInteger.
     */
    public @View int hashCode() {
        return val.hashCode();
    }

    /**
     * Returns the decimal String representation of this UnsignedBigInteger.
     *
     * @return decimal String representation of this UnsignedBigInteger.
     */
    public @View String toString() {
        return val.toString();
    }

    /**
     * Returns the String representation of this UnsignedBigInteger in the given radix.
     *
     * @param radix radix of the String representation.
     * @return representation of this UnsignedBigInteger in the given radix.
     */
    public @View String toString(int radix) {
        return val.toString(radix);
    }

    /**
     * Returns a UnsignedBigInteger whose value is equal to that of the specified long.
     *
     * @param value value of the UnsignedBigInteger to return.
     * @return a UnsignedBigInteger with the specified value.
     */
    public static UnsignedBigInteger valueOf(long value) {
        return new UnsignedBigInteger(BigInteger.valueOf(value)); //The constructor guarantees the check "value >= 0"
    }
}