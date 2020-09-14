package io.takamaka.code.whitelisting.internal.database.java.math;

public abstract class BigInteger {
	public static java.math.BigInteger ONE;
	public static java.math.BigInteger TEN;
	public static java.math.BigInteger ZERO;

	public BigInteger(String val){} //new
	public BigInteger(String val, int radix){} //new

	public abstract int signum();
	public abstract java.math.BigInteger valueOf(long val);
	public abstract boolean equals(java.lang.Object other);
	public abstract int hashCode();
	public abstract java.lang.String toString();
	public abstract java.lang.String toString(int radix); //new
	public abstract byte[] toByteArray();
	public abstract int intValue(); //new
	public abstract int compareTo(java.math.BigInteger other);
	public abstract java.math.BigInteger add(java.math.BigInteger val);
	public abstract java.math.BigInteger subtract(java.math.BigInteger val);
	public abstract java.math.BigInteger multiply(java.math.BigInteger val);
	public abstract java.math.BigInteger divide(java.math.BigInteger val);
	public abstract java.math.BigInteger mod(java.math.BigInteger m); //new
	public abstract java.math.BigInteger pow(int exponent); //new
	public abstract java.math.BigInteger max(java.math.BigInteger val); //new
	public abstract java.math.BigInteger min(java.math.BigInteger val); //new
}