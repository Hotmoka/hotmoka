package io.takamaka.code.whitelisting.internal.database.java.math;

public abstract class BigInteger {
	public static java.math.BigInteger ONE;
	public static java.math.BigInteger TWO;
	public static java.math.BigInteger TEN;
	public static java.math.BigInteger ZERO;

	public BigInteger(java.lang.String val) {}
	public BigInteger(java.lang.String val, int radix) {}

	public abstract int signum();
	public abstract java.math.BigInteger valueOf(long val);
	public abstract boolean equals(java.lang.Object other);
	public abstract int hashCode();
	public abstract java.lang.String toString();
	public abstract java.lang.String toString(int radix);
	public abstract byte[] toByteArray();
	public abstract int compareTo(java.math.BigInteger other);
	public abstract java.math.BigInteger add(java.math.BigInteger val);
	public abstract java.math.BigInteger subtract(java.math.BigInteger val);
	public abstract java.math.BigInteger multiply(java.math.BigInteger val);
	public abstract java.math.BigInteger divide(java.math.BigInteger val);
	public abstract java.math.BigInteger[] divideAndRemainder(java.math.BigInteger divisor);
	public abstract java.math.BigInteger mod(java.math.BigInteger m);
	public abstract java.math.BigInteger pow(int exponent);
	public abstract java.math.BigInteger max(java.math.BigInteger val);
	public abstract java.math.BigInteger min(java.math.BigInteger val);
}