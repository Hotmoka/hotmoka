package io.takamaka.code.math;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * This class represents an unsigned big integer.
 * It is a valid alternative to the {@code uint256} type that exists in Solidity.
 * It implements most common constructors of {@link java.math.BigInteger} and some useful functions.
 */
@Exported
public class UnsignedBigInteger extends Storage implements Comparable<UnsignedBigInteger>{

	/**
     * Stored value (guaranteed to be non-negative)
     */
    private final BigInteger val;

    /**
     * Creates an unsigned big integer from a non-negative {@link java.math.BigInteger}.
     *
     * @param val {@link java.math.BigInteger}
     */
    public UnsignedBigInteger(BigInteger val) {
        require(val.signum() >= 0, "Illegal value: negative value"); // val >= 0
        this.val = val;
    }

    /**
     * Creates an unsigned big integer from a string.
     *
     * @param val the string, that must represent a non-negative integer
     */
    public UnsignedBigInteger(String val) {
        this(new BigInteger(val));
    }

    /**
     * Creates an unsigned big integer from a string and a radix.
     *
     * @param val the string, that must represent a non-negative integer in {@code radix} base
     * @param radix the coding base of {@code val}, such as 2 for binary
     */
    public UnsignedBigInteger(String val, int radix) {
        this(new BigInteger(val, radix));
    }

    /**
     * A constructor for internal use, that does not verify that {@code val} is non-negative.
     *
     * @param val the value to allocate (it is assumed that it is non-negative)
     * @param dummy disambiguator for the homonymous public constructor
     */
    private UnsignedBigInteger(BigInteger val, boolean dummy){
        this.val = val;
    }

    /**
     * Returns an unsigned big integer whose value is {@code this} + {@code other}.
     *
     * @param other value that will be added to this unsigned big integer
     * @return the addition {@code this} + {@code other}
     */
    public UnsignedBigInteger add(UnsignedBigInteger other) {
        return new UnsignedBigInteger(val.add(other.val), true);
    }

    /**
     * Returns an unsigned big integer whose value is {@code this} - {@code other}.
     *
     * @param other value that will be subtracted from this unsigned big integer
     * @param errorMessage the message of the requirement exception generated if {@code other} is greater than {@code this}
     * @return the difference {@code this} - {@code other}
     * @throws RequirementViolationException if {@code other} is greater than {@code this}
     */
    public UnsignedBigInteger subtract(UnsignedBigInteger other, String errorMessage) {
        BigInteger diff = val.subtract(other.val);
        require(diff.signum() >= 0, errorMessage); // this.val >= other.val
        return new UnsignedBigInteger(diff, true);
    }

    /**
     * Returns an unsigned big integer whose value is {@code this} - {@code other}.
     *
     * @param other value that will be subtracted from this unsigned big integer
     * @return the difference {@code this} - {@code other}
     * @throws RequirementViolationException if {@code other} is greater than {@code this}
     */
    public UnsignedBigInteger subtract(UnsignedBigInteger other) {
        return subtract(other, "Illegal operation: subtraction overflow");
    }

    /**
     * Returns an unsigned big integer whose value is {@code this} * {@code other}.
     *
     * @param other value that will be multiplied to this unsigned big integer
     * @return the multiplication {@code this} * {@code other}
     */
    public UnsignedBigInteger multiply(UnsignedBigInteger other) {
        return new UnsignedBigInteger(val.multiply(other.val), true);
    }

    /**
     * Returns an unsigned big integer whose value is {@code this} / {@code other}.
     *
     * @param other the divisor
     * @param errorMessage the message of the requirement exception generated if {@code other} is zero
     * @return the division {@code this} / {@code other}
     * @throws RequirementViolationException if {@code other} is zero
     */
    public UnsignedBigInteger divide(UnsignedBigInteger other, String errorMessage) {
        require(other.val.signum() != 0, errorMessage); // other.val > 0
        return new UnsignedBigInteger(val.divide(other.val), true);
    }

    /**
     * Returns an unsigned big integer whose value is {@code this} / {@code other}.
     *
     * @param other the divisor
     * @return the division {@code this} / {@code other}
     * @throws RequirementViolationException if {@code other} is zero
     */
    public UnsignedBigInteger divide(UnsignedBigInteger other) {
        return divide(other, "Illegal operation: division by zero");
    }

    /**
     * Returns an unsigned big integer whose value is {@code this} mod {@code divisor} (integer remainder).
     *
     * @param divisor the divisor
     * @param errorMessage the message of the requirement exception generated if {@code divisor} is zero
     * @return the remainder {@code this} mod {@code divisor}
     * @throws RequirementViolationException if {@code divisor} is zero
     */
    public UnsignedBigInteger mod(UnsignedBigInteger divisor, String errorMessage) {
        require(divisor.val.signum() != 0, errorMessage); // other.val > 0
        return new UnsignedBigInteger(val.mod(divisor.val), true);
    }

    /**
     * Returns an unsigned big integer whose value is {@code this} mod {@code divisor} (integer remainder).
     *
     * @param divisor the divisor
     * @return the remainder {@code this} mod {@code divisor}
     * @throws RequirementViolationException if {@code divisor} is zero
     */
    public UnsignedBigInteger mod(UnsignedBigInteger divisor) {
        return mod(divisor, "Illegal operation: modulo by zero");
    }

    /**
     * Returns an unsigned big integer whose value is {@code this} elevated to the power of {@code exponent}.
     * Note that the exponent is an integer rather than an unsigned big integer.
     *
     * @param exponent the exponent
     * @return the power
     */
    public UnsignedBigInteger pow(int exponent) {
        return new UnsignedBigInteger(val.pow(exponent), true);
    }

    /**
     * Returns the maximum between this unsigned big integer and another.
     *
     * @param other the other unsigned big integer
     * @return the maximum
     */
    public UnsignedBigInteger max(UnsignedBigInteger other) {
        return new UnsignedBigInteger(val.max(other.val), true);
    }

    /**
     * Returns the minimum between this unsigned big integer and another.
     *
     * @param other the other unsigned big integer
     * @return the minimum
     */
    public UnsignedBigInteger min(UnsignedBigInteger other) {
        return new UnsignedBigInteger(val.min(other.val), true);
    }

    /**
     * Compares this unsigned big integer with another.
     *
     * @param other the other unsigned big integer
     * @return negative, zero or positive as this is smaller, equal or larger than {@code other}
     */
    public @View int compareTo(UnsignedBigInteger other) {
        return val.compareTo(other.val);
    }

    /**
     * Compares this unsigned big integer with the specified Object for equality.
     *
     * @param other the other object
     * @return true if and only if the specified object is an {@link UnsignedBigInteger} whose value is numerically equal to
     *         the value of this
     */
    @Override
    public @View boolean equals(Object other) {
        return other == this || (other instanceof UnsignedBigInteger && val.equals(((UnsignedBigInteger) other).val));
    }

    @Override
    public @View int hashCode() {
        return val.hashCode();
    }

    /**
     * Returns the {@link java.math.BigInteger} corresponding to this unsigned big integer.
     * Note: it might be unsafe (make safe use of it).
     *
     * @return the {@link java.math.BigInteger}
     */
    public @View BigInteger toBigInteger() {
        return val;
    }

    /**
     * Returns the decimal string representation of this unsigned big integer.
     *
     * @return the representation
     */
    @Override
    public @View String toString() {
        return val.toString();
    }

    /**
     * Returns the string representation of this unsigned big integer in the given radix.
     *
     * @param radix the radix
     * @return the representation
     */
    public @View String toString(int radix) {
        return val.toString(radix);
    }

    /**
     * Returns an unsigned big integer whose value is equal to that of the given long value.
     *
     * @param value the long value
     * @return the unsigned big integer
     */
    public static UnsignedBigInteger valueOf(long value) {
        return new UnsignedBigInteger(BigInteger.valueOf(value)); //The constructor guarantees the check "value >= 0"
    }
}