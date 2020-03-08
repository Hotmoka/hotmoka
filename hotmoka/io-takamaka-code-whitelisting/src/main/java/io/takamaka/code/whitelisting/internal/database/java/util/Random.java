package io.takamaka.code.whitelisting.internal.database.java.util;

public abstract class Random {

	public Random(long seed) {}

	public abstract void setSeed(long seed);
	public abstract void nextBytes(byte[] bytes);
	public abstract int nextInt();
	public abstract int nextInt(int bound);
	public abstract long nextLong();
	public abstract boolean nextBoolean();
	public abstract float nextFloat();
	public abstract double nextDouble();
	public abstract double nextGaussian();
}