package io.takamaka.code.whitelisting.internal.database.version0.java.lang;

public interface Math {
	int addExact​(int x, int y);
	long addExact​(long x, long y);
	int decrementExact​(int a);
	long decrementExact​(long a);
	int incrementExact​(int a);
	long incrementExact​(long a);
	int multiplyExact​(int x, int y);
	long multiplyExact​(long x, int y);
	long multiplyExact​(long x, long y);
	long multiplyFull​(int x, int y);
	long multiplyHigh​(long x, long y);
	int negateExact​(int a);
	long negateExact​(long a);
	int subtractExact​(int x, int y);
	long subtractExact​(long x, long y);
	int toIntExact​(long value);
}