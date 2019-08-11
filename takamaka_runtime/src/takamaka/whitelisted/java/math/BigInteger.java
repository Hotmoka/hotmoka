package takamaka.whitelisted.java.math;

public abstract class BigInteger {
	public static java.math.BigInteger ONE;
	public static java.math.BigInteger TEN;
	public static java.math.BigInteger ZERO;
	public abstract int signum();
	public abstract byte[] toByteArray();
}