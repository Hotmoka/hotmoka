package io.takamaka.code.whitelisting.internal.database.java.math;

public abstract class BigInteger {
	public static java.math.BigInteger ONE;
	public static java.math.BigInteger TEN;
	public static java.math.BigInteger ZERO;
	public abstract int signum();
	public abstract java.math.BigInteger valueOf(long val);
	public abstract byte[] toByteArray();
	public abstract int compareTo(java.math.BigInteger other);
	public abstract java.math.BigInteger add(java.math.BigInteger val);
	public abstract java.math.BigInteger subtract(java.math.BigInteger val);
	public abstract java.math.BigInteger multiply(java.math.BigInteger val);
	public abstract java.math.BigInteger divide(java.math.BigInteger val);
}